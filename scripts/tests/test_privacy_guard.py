"""Behavioral tests in disposable Git repositories; never touch the real index."""
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

SOURCE = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('privacy_guard', SOURCE / 'scripts/privacy_guard.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class PrivacyGateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='privacy-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.env = {k: v for k, v in os.environ.items() if not k.startswith('GIT_')}
        # Keep actual Git hooks, but isolate global identity/config and signing settings.
        self.env.update(GIT_CONFIG_NOSYSTEM='1', GIT_CONFIG_GLOBAL=os.devnull,
                        GIT_TERMINAL_PROMPT='0', PYTHONIOENCODING='utf-8')
        self.git('init', '-q')
        self.git('config', 'user.name', 'Demo Developer')
        self.git('config', 'user.email', 'dev@example.com')
        (self.root / 'scripts').mkdir()
        shutil.copy2(SOURCE / 'scripts/privacy_guard.py', self.root / 'scripts/privacy_guard.py')
        shutil.copy2(SOURCE / 'scripts/privacy-ocr.ps1', self.root / 'scripts/privacy-ocr.ps1')
        shutil.copytree(SOURCE / '.githooks', self.root / '.githooks')
        self.policy = json.loads((SOURCE / module.POLICY).read_text())
        self.policy['blocked_token_sha256'].append(module.digest('HiddenPerson'))
        self.write_policy()
        self.git('add', '.')
        self.git('commit', '-qm', 'Initial safe tooling')

    def git(self, *args, ok=True):
        result = subprocess.run(['git', *args], cwd=self.root, env=self.env, capture_output=True)
        if ok:
            self.assertEqual(result.returncode, 0, result.stderr.decode(errors='replace'))
        return result

    def run_guard(self, *args, stdin=None):
        return subprocess.run([sys.executable, str(self.root / 'scripts/privacy_guard.py'), *args],
                              cwd=self.root, env=self.env, input=stdin, capture_output=True)

    def write_policy(self):
        (self.root / module.POLICY).write_text(json.dumps(self.policy), encoding='utf8')

    def write(self, path, value):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(value, bytes):
            target.write_bytes(value)
        else:
            target.write_text(value, encoding='utf8')

    def assert_block(self, result, category):
        self.assertEqual(result.returncode, 1, result.stderr.decode(errors='replace'))
        self.assertIn(category.encode(), result.stdout)

    def test_safe_staged_code_and_placeholders(self):
        self.write('app.java', 'String password = dto.getPassword();\nString token = UUID.randomUUID().toString();')
        self.write('config.yml', 'password: ${DB_PASSWORD}\nsecret: <random-value>\nhost: 127.0.0.1')
        self.git('add', '.')
        result = self.run_guard('staged')
        self.assertEqual(result.returncode, 0, result.stdout.decode())

    def test_code_expressions_and_sql_bindings_are_not_credentials(self):
        self.write('app.java', 'String password = service.lookup();\nString token = (String) value;\nString sql = "token = #{token}";')
        self.write('app.js', 'const changePassword = (data) => send(data);\nconst fallbackToken = "--color-ink";')
        self.git('add', '.')
        result = self.run_guard('staged')
        self.assertEqual(result.returncode, 0, result.stdout.decode())

    def test_ocr_split_identifier_is_detected(self):
        guard = module.Guard(self.root, self.policy)
        try:
            guard.ocr_contents('Hidden Person', 'image.png', 'image.png')
            self.assertIn(('blocked-personal-identifier', 'image.png!OCR', 1), guard.findings)
        finally:
            guard.close()

    def test_placeholder_environment_default_is_still_insecure(self):
        self.write('config.yml', 'secret: ' + '$' + '{JWT_SECRET:please_change_me}')
        self.git('add', '.')
        self.assert_block(self.run_guard('staged'), 'credential-default')

    def test_clean_history_and_push_succeed(self):
        result = self.run_guard('history', 'HEAD')
        self.assertEqual(result.returncode, 0, result.stdout.decode())
        self.assertEqual(self.run_guard('install').returncode, 0)
        with tempfile.TemporaryDirectory(prefix='privacy-receiver-') as receiver:
            subprocess.run(['git', 'init', '--bare', '-q', receiver], env=self.env, check=True)
            self.git('push', receiver, 'HEAD:refs/heads/main')

    def test_scans_index_not_clean_worktree(self):
        secret = 'gh' + 'p_' + 'Z' * 36
        self.write('notes.txt', secret)
        self.git('add', 'notes.txt')
        self.write('notes.txt', 'clean on disk')
        result = self.run_guard('staged')
        self.assert_block(result, 'secret-signature')
        self.assertNotIn(secret.encode(), result.stdout + result.stderr)

    def test_unstaged_secret_does_not_change_staged_snapshot(self):
        self.write('notes.txt', 'safe snapshot')
        self.git('add', 'notes.txt')
        self.write('notes.txt', 'gh' + 'p_' + 'Z' * 36)
        self.assertEqual(self.run_guard('staged').returncode, 0)

    def test_short_password_and_environment_default(self):
        env = '$' + '{JWT_SECRET:' + 'unsafe-default-value' + '}'
        self.write('config.yml', 'pass' + 'word: ' + str(987000 + 654) + '\nsecret: ' + env)
        self.git('add', '.')
        result = self.run_guard('staged')
        self.assert_block(result, 'credential-literal')
        self.assertIn(b'credential-default', result.stdout)

    def test_identity_and_message(self):
        self.git('config', 'user.email', 'private@' + 'mail.invalid')
        self.assert_block(self.run_guard('identity'), 'personal-email')
        self.git('config', 'user.email', 'dev@example.com')
        self.git('config', 'user.name', 'HiddenPerson')
        self.assert_block(self.run_guard('identity'), 'blocked-personal-identifier')
        self.git('config', 'user.name', 'Demo Developer')
        self.write('message', 'removed gh' + 'p_' + 'Z' * 36)
        result = self.run_guard('commit-msg', 'message')
        self.assert_block(result, 'secret-signature')

    def test_blocked_domain_inside_filename(self):
        self.policy['blocked_token_sha256'].append(module.digest('private-host.example.invalid'))
        self.write_policy()
        self.write('private-host.example.invalid.conf.example', 'safe body')
        self.git('add', '.')
        self.assert_block(self.run_guard('staged'), 'blocked-personal-identifier')

    def test_deleted_secret_remains_blocked_in_history(self):
        self.write('notes.txt', 'gh' + 'p_' + 'Z' * 36)
        self.git('add', '.')
        self.git('commit', '-qm', 'Add data')
        self.git('rm', '-q', 'notes.txt')
        self.git('commit', '-qm', 'Remove data')
        result = self.run_guard('history', 'HEAD')
        self.assert_block(result, 'secret-signature')
        tip = self.git('rev-parse', 'HEAD').stdout.strip().decode()
        # Even an up-to-date remote base must not grandfather contaminated ancestors.
        line = f'refs/heads/main {tip} refs/heads/main {tip}\n'.encode()
        self.assert_block(self.run_guard('pre-push', stdin=line), 'secret-signature')

    def test_all_pushed_refs_and_annotated_tags_are_checked(self):
        self.git('tag', '-a', 'bad-tag', '-m', 'Removed gh' + 'p_' + 'Z' * 36)
        tag = self.git('rev-parse', 'bad-tag').stdout.strip().decode()
        tip = self.git('rev-parse', 'HEAD').stdout.strip().decode()
        lines = f'refs/heads/main {tip} refs/heads/main {"0" * 40}\nrefs/tags/bad-tag {tag} refs/tags/bad-tag {"0" * 40}\n'
        self.assert_block(self.run_guard('pre-push', stdin=lines.encode()), 'secret-signature')

    def test_tag_pointing_at_blob_fails_closed(self):
        blob = self.git('rev-parse', 'HEAD:scripts/privacy_guard.py').stdout.strip().decode()
        result = self.run_guard('history', blob)
        self.assertEqual(result.returncode, 2)

    def test_renamed_sensitive_file_is_checked(self):
        self.write('safe.txt', 'ordinary config')
        self.git('add', '.')
        self.git('commit', '-qm', 'Add safe file')
        self.git('mv', 'safe.txt', '.env')
        self.assert_block(self.run_guard('staged'), 'forbidden-file')

    def test_binary_requires_exact_content_approval(self):
        data = b'%PDF-review-fixture'
        self.write('guide.pdf', data)
        self.git('add', '.')
        self.assert_block(self.run_guard('staged'), 'binary-review-required')
        self.policy['reviewed_assets'] = [{'path':'guide.pdf', 'sha256':hashlib.sha256(data).hexdigest(),
                                          'reason':'Synthetic test fixture', 'reviewer':'Demo'}]
        self.write_policy()
        self.git('add', '.')
        self.assertEqual(self.run_guard('staged').returncode, 0)
        self.write('guide.pdf', data + b'changed')
        self.git('add', '.')
        self.assert_block(self.run_guard('staged'), 'binary-review-required')

    def test_unstaged_policy_cannot_approve_staged_asset(self):
        data = b'%PDF-review-fixture'
        self.write('guide.pdf', data)
        self.git('add', 'guide.pdf')
        self.policy['reviewed_assets'] = [{'path':'guide.pdf', 'sha256':hashlib.sha256(data).hexdigest(),
                                          'reason':'Synthetic test fixture', 'reviewer':'Demo'}]
        self.write_policy()
        self.assert_block(self.run_guard('staged'), 'binary-review-required')

    def test_docx_custom_metadata_is_not_hidden_by_binary_approval(self):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as archive:
            archive.writestr('word/document.xml', '<document>safe body</document>')
            archive.writestr('docProps/custom.xml', '<Properties><property name="device">encoded-value</property></Properties>')
        data = stream.getvalue()
        self.write('guide.docx', data)
        self.policy['reviewed_assets'] = [{'path':'guide.docx', 'sha256':hashlib.sha256(data).hexdigest(),
                                          'reason':'Synthetic test fixture', 'reviewer':'Demo'}]
        self.write_policy()
        self.git('add', '.')
        self.assert_block(self.run_guard('staged'), 'document-custom-metadata')

    def test_utf16_and_unreadable_text(self):
        self.write('utf16.txt', ('gh' + 'p_' + 'Z' * 36).encode('utf16'))
        self.write('invalid.txt', b'\xffinvalid')
        self.git('add', '.')
        result = self.run_guard('staged')
        self.assert_block(result, 'secret-signature')
        self.assertIn(b'text-encoding-unreadable', result.stdout)

    def test_phone_ip_and_private_ipv6(self):
        self.write('notes.txt', 'contact ' + '138' + '0013' + '8000' + '\nhost: ' + '.'.join(('10', '22', '33', '44')) + '\n"fd12' + ':3456::1"')
        self.git('add', '.')
        result = self.run_guard('staged')
        self.assert_block(result, 'phone-number')
        self.assertIn(b'network-address', result.stdout)

    def test_lfs_pointer_fails_closed(self):
        self.write('image.png', 'version https://git-lfs.github.com/spec/v1\noid sha256:' + 'a'*64 + '\nsize 10\n')
        self.git('add', '.')
        self.assert_block(self.run_guard('staged'), 'lfs-object-not-inspected')

    def test_diagnostic_redacts_sensitive_filename(self):
        guard = module.Guard(self.root, self.policy)
        try:
            value = 'sensitive-filename-value'
            source = 'config/' + 'password' + '=' + value + '.txt'
            self.assertNotIn(value, guard.safe_source(source))
        finally:
            guard.close()

    def test_shallow_history_and_missing_policy_fail_closed(self):
        tip = self.git('rev-parse', 'HEAD').stdout
        (self.root / '.git/shallow').write_bytes(tip)
        self.assertEqual(self.run_guard('history', 'HEAD').returncode, 2)
        (self.root / module.POLICY).unlink()
        self.assertEqual(self.run_guard('worktree').returncode, 2)

    def test_deletion_only_push_is_allowed(self):
        line = f'(delete) {"0" * 40} refs/heads/old {"a" * 40}\n'.encode()
        self.assertEqual(self.run_guard('pre-push', stdin=line).returncode, 0)
        self.assertEqual(self.run_guard('pre-push', stdin=b'invalid\n').returncode, 2)

    def test_installer_preserves_existing_hooks(self):
        hook = self.root / '.git/hooks/pre-commit'
        hook.write_text('#!/bin/sh\nexit 0\n')
        result = self.run_guard('install')
        self.assertEqual(result.returncode, 2)
        self.assertEqual(hook.read_text(), '#!/bin/sh\nexit 0\n')

    def test_real_commit_hook_and_safe_commit(self):
        self.assertEqual(self.run_guard('install').returncode, 0)
        self.write('notes.txt', 'safe content')
        self.git('add', '.')
        self.git('commit', '-qm', 'Safe commit')
        self.write('notes.txt', 'gh' + 'p_' + 'Z' * 36)
        self.git('add', '.')
        result = self.git('commit', '-qm', 'Unsafe commit', ok=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(b'secret-signature', result.stdout + result.stderr)

    def test_real_push_hook_keeps_receiver_clean(self):
        self.write('notes.txt', 'gh' + 'p_' + 'Z' * 36)
        self.git('add', '.')
        self.git('commit', '-qm', 'Contaminated ancestor')
        self.git('rm', '-q', 'notes.txt')
        self.git('commit', '-qm', 'Clean tip')
        self.assertEqual(self.run_guard('install').returncode, 0)
        with tempfile.TemporaryDirectory(prefix='privacy-receiver-') as receiver:
            subprocess.run(['git','init','--bare','-q',receiver], env=self.env, check=True)
            result = self.git('push', receiver, 'HEAD:refs/heads/main', ok=False)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn(b'secret-signature', result.stdout + result.stderr)
            refs = subprocess.check_output(['git','-C',receiver,'for-each-ref'],env=self.env)
            self.assertEqual(refs, b'')


if __name__ == '__main__':
    unittest.main()
