"""Local, offline privacy gate. Python 3.11+, no third-party dependencies.

Read Git objects, not diffs: a later deletion must not hide an earlier leak.
Diagnostics contain rules and locations only, never matched values.
"""
from __future__ import annotations

import argparse
from collections import Counter
import fnmatch
import hashlib
import io
import ipaddress
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unicodedata
import xml.etree.ElementTree as ET
import zipfile

POLICY = '.privacy-policy.json'
MAX_BYTES = 25 * 1024 * 1024
EMAIL = re.compile(r'[\w.%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}')
TOKENS = re.compile(r'[\w]+(?:[.@+\-][\w]+)*', re.UNICODE)
KNOWN = re.compile(
    r'-----BEGIN (?:[A-Z0-9]+ )*PRIVATE KEY-----|'
    r'(?:AKIA|ASIA)[A-Z0-9]{16}|(?:gh[pousr]_|github_pat_)[A-Za-z0-9_]{20,}|'
    r'sk-(?:proj-)?[A-Za-z0-9_-]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|'
    r'AIza[A-Za-z0-9_-]{30,}|sk_live_[A-Za-z0-9]{16,}|'
    r'eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}|'
    r'\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}')
ASSIGN = re.compile(
    r'''(?ix)(?:[\w.-]*[_.-])?(?:password|passwd|pwd|secret|token|apikey|api_key|api-key|access_key|private_key)
    ["']?\s*[:=]\s*(?P<quote>["']?)(?P<value>[^\s"',;)}]+)''')
IP = re.compile(r'(?<![\w.])(?:\d{1,3}\.){3}\d{1,3}(?![\w.]|\.\d)')
PHONE = re.compile(r'(?<![\w])(?:\+?86[ -]?)?1[3-9]\d{9}(?![\w])')
PATH = re.compile(r'(?i)(?:[a-z]:[/\\]+[U]sers[/\\]+[^/\\\s"\x27]+|/[U]sers/[^/\s]+|/[h]ome/(?!blog\b|app\b|www\b)[^/\s]+|[a-z]:[/\\]+Claude-Code-workplace\b)')
URL_CREDENTIAL = re.compile(r'(?i)(?:https?|ssh|mysql|postgres(?:ql)?|redis)://[^\s/@:]+:[^\s/@]+@')
IMAGE = {'.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp', '.tiff', '.ico'}
BINARY = IMAGE | {'.docx', '.xlsx', '.pptx', '.pdf', '.doc', '.xls', '.ppt', '.zip', '.gz', '.tgz', '.7z', '.rar', '.jar', '.war', '.class', '.ttf', '.otf', '.woff', '.woff2', '.mp4', '.mp3', '.wav', '.xdb'}
CODE = {'.py', '.js', '.mjs', '.cjs', '.ts', '.vue', '.java', '.ps1'}
PLACEHOLDER = re.compile(r'(?i)^(?:\$|#\{|%%|%[A-Z_]+%|<|your[_-]|example[_-]|dummy[_-]|test[_-]|fixture[_-]|synthetic[_-]|replace[_-]|change[_-]?me|please[_-]|any_long|!BOOTSTRAP_REQUIRED!|替换|你的|填写|process\.|this\.|props\.|form\.|dto\.)')
DOC_NETWORKS = tuple(ipaddress.ip_network(x) for x in ('192.0.2.0/24', '198.51.100.0/24', '203.0.113.0/24'))


class GuardError(Exception):
    pass


def digest(value: str) -> str:
    return hashlib.sha256(unicodedata.normalize('NFKC', value).casefold().encode()).hexdigest()


def git(root: Path, *args: str, allow_failure=False) -> bytes:
    result = subprocess.run(['git', '-C', str(root), *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode and not allow_failure:
        raise GuardError('Git operation failed: ' + args[0])
    return result.stdout if result.returncode == 0 else b''


def decode(data: bytes) -> str:
    # Explicit support for UTF-16 exports; never silently skip undecodable text.
    if data.startswith((b'\xff\xfe', b'\xfe\xff')):
        return data.decode('utf-16')
    return data.decode('utf-8-sig')


def load_policy(root: Path, staged=False) -> dict:
    raw = git(root, 'show', ':' + POLICY) if staged else (root / POLICY).read_bytes()
    policy = json.loads(decode(raw))
    if policy.get('version') != 1:
        raise GuardError('Unsupported privacy policy version')
    for key in ('blocked_token_sha256', 'allowed_emails', 'forbidden_paths', 'ip_exceptions', 'reviewed_assets'):
        if not isinstance(policy.get(key), list):
            raise GuardError('Invalid privacy policy: ' + key)
    for h in policy['blocked_token_sha256']:
        if not isinstance(h, str) or not re.fullmatch('[a-f0-9]{64}', h):
            raise GuardError('Invalid blocked fingerprint')
    for a in policy['reviewed_assets']:
        if not isinstance(a, dict):
            raise GuardError('Invalid asset approval')
        if not all(isinstance(a.get(k), str) and a[k].strip() for k in ('path', 'sha256', 'reason', 'reviewer')):
            raise GuardError('Asset approvals require path, sha256, reason and reviewer')
        if not re.fullmatch('[a-f0-9]{64}', a['sha256']) or any(c in a['path'] for c in '*?['):
            raise GuardError('Asset approvals require exact paths and SHA-256')
    return policy


class Guard:
    def __init__(self, root: Path, policy: dict):
        self.root, self.policy = root, policy
        self.findings: set[tuple[str, str, int]] = set()
        self.seen: set[tuple[str, str, str]] = set()
        self.scanned = 0
        self.batch = subprocess.Popen(['git', '-C', str(root), 'cat-file', '--batch'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)

    def close(self):
        self.batch.stdin.close()
        self.batch.stdout.close()
        self.batch.wait()

    def add(self, rule: str, source: str, line=1):
        self.findings.add((rule, source, line))

    def safe_source(self, source: str) -> str:
        source = KNOWN.sub('[redacted]', EMAIL.sub('[email]', source))
        source = ASSIGN.sub('[credential-field]', source)
        source = PHONE.sub('[phone]', IP.sub('[address]', source))
        source = TOKENS.sub(lambda m: '[identifier]' if digest(m[0]) in self.policy['blocked_token_sha256'] else m[0], source)
        return json.dumps(source[:400], ensure_ascii=True)

    def text(self, text: str, source: str, path=''):
        text = unicodedata.normalize('NFKC', text)
        suffix = Path(path).suffix.lower()
        for n, line in enumerate(text.splitlines(), 1):
            if any(not (m[0].startswith('$2') and 'dummyhash' in m[0]) for m in KNOWN.finditer(line)):
                self.add('secret-signature', source, n)
            if URL_CREDENTIAL.search(line):
                self.add('url-credentials', source, n)
            for mail in EMAIL.findall(line):
                domain = mail.rsplit('@', 1)[1].lower()
                if (domain not in {'example.com', 'example.org', 'example.net', 'example.invalid'}
                        and not domain.endswith('.example')
                        and domain != 'users.noreply.github.com'
                        and mail.lower() not in self.policy['allowed_emails']):
                    self.add('personal-email', source, n)
            for token in TOKENS.findall(line):
                if digest(token) in self.policy['blocked_token_sha256']:
                    self.add('blocked-personal-identifier', source, n)
                # Catch subdomains and domain-bearing filenames (including .conf.example).
                if '.' in token:
                    parts = token.split('.')
                    if any(digest('.'.join(parts[i:j])) in self.policy['blocked_token_sha256']
                           for i in range(len(parts)) for j in range(i + 1, len(parts) + 1)):
                        self.add('blocked-personal-identifier', source, n)
            if PHONE.search(line):
                self.add('phone-number', source, n)
            if PATH.search(line):
                self.add('personal-local-path', source, n)
            for match in IP.finditer(line):
                # SVG path coordinates are not addresses. Other code/test literals are checked.
                if re.search(r'\bd=["\x27]', line[:match.start()]):
                    continue
                try:
                    address = ipaddress.ip_address(match[0])
                except ValueError:
                    continue
                if address.is_loopback or address.is_unspecified or any(address in net for net in DOC_NETWORKS):
                    continue
                if any((e.get('path') == path or path == POLICY) and e.get('value') == str(address) and e.get('reason') for e in self.policy['ip_exceptions']):
                    continue
                self.add('network-address', source, n)
            for match in ASSIGN.finditer(line):
                value = match['value']
                if PLACEHOLDER.match(value) or value.startswith('--') or value in {'null', 'None', 'true', 'false', '?', '[]', '{}', '...'}:
                    continue
                # Unquoted code expressions are references, not literal credentials.
                if suffix in CODE and not match['quote'] and not value.isdecimal():
                    continue
                if len(value) >= 3:
                    self.add('credential-literal', source, n)
            for match in re.finditer(r'(?i)\$\{[^}:]*(?:password|secret|token|key)[^}:]*:([^}]+)\}', line):
                value = match[1]
                if not value.lstrip('-').startswith(('$', '%')):
                    self.add('credential-default', source, n)
            for match in re.finditer(r'''(?:\[|["'])([a-fA-F0-9:]+(?::[a-fA-F0-9.]+)+)(?:\]|["'])''', line):
                try:
                    address = ipaddress.ip_address(match[1])
                except ValueError:
                    continue
                if address.is_loopback or address.is_unspecified or address in ipaddress.ip_network('2001:db8::/32'):
                    continue
                if any((e.get('path') == path or path == POLICY) and e.get('value') == str(address) and e.get('reason') for e in self.policy['ip_exceptions']):
                    continue
                self.add('network-address', source, n)

    def identity(self, identity: str, source: str):
        self.text(identity, source)
        if not re.search(r'<[^<>\s]+@(?:users\.noreply\.github\.com|example\.(?:com|org|net|invalid))>', identity):
            if not any('<' + mail + '>' in identity for mail in self.policy['allowed_emails']):
                self.add('identity-use-private-email', source)

    def current_identity(self):
        for kind in ('AUTHOR', 'COMMITTER'):
            self.identity(decode(git(self.root, 'var', 'GIT_' + kind + '_IDENT')), kind.lower())

    def object(self, oid: str) -> bytes | None:
        if not re.fullmatch('[a-f0-9]{40}|[a-f0-9]{64}', oid):
            raise GuardError('Invalid object identifier')
        self.batch.stdin.write((oid + '\n').encode())
        self.batch.stdin.flush()
        fields = self.batch.stdout.readline().split()
        if len(fields) != 3:
            raise GuardError('Cannot read Git object')
        size = int(fields[2])
        if size > MAX_BYTES:
            left = size
            while left:
                chunk = self.batch.stdout.read(min(left, 65536))
                if not chunk:
                    raise GuardError('Incomplete Git object')
                left -= len(chunk)
            data = None
        else:
            data = self.batch.stdout.read(size)
            if len(data) != size:
                raise GuardError('Incomplete Git object')
        if self.batch.stdout.read(1) != b'\n':
            raise GuardError('Invalid Git object framing')
        return data

    def document(self, data: bytes, source: str, path: str):
        try:
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                infos = archive.infolist()
                if len(infos) > 2000 or sum(x.file_size for x in infos) > MAX_BYTES:
                    self.add('document-expansion-limit', source)
                    return
                for item in infos:
                    name = item.filename
                    if name.endswith(('.xml', '.rels')):
                        xml = decode(archive.read(item))
                        tree = ET.fromstring(xml)
                        contents = ''.join(tree.itertext())
                        self.text(contents, source + '!' + name, path)
                        # Also inspect relationship targets, comments and custom properties.
                        self.text(xml, source + '!' + name, path)
                        if name == 'docProps/custom.xml' and list(tree):
                            self.add('document-custom-metadata', source + '!' + name)
                        for node in tree.iter():
                            if node.tag.rsplit('}', 1)[-1] in {'creator', 'lastModifiedBy'} and (node.text or '').strip():
                                self.add('document-author-metadata', source + '!' + name)
                    elif '/embeddings/' in name or 'vbaProject' in name:
                        self.add('document-embedded-content', source + '!' + name)
        except (ValueError, zipfile.BadZipFile, ET.ParseError, UnicodeError, RuntimeError):
            self.add('document-unreadable', source)

    def image_text(self, data: bytes, source: str, path: str):
        """Optional local OCR. Review gate still applies when OCR is unavailable."""
        command = shutil.which('tesseract')
        windows_ocr = os.name == 'nt' and shutil.which('powershell')
        if not command and not windows_ocr:
            self.add('ocr-unavailable-use-manual-review', source)
            return
        with tempfile.TemporaryDirectory(prefix='privacy-ocr-') as folder:
            image = Path(folder) / ('input' + Path(path).suffix)
            image.write_bytes(data)
            try:
                args = ([command, str(image), 'stdout', '-l', 'eng+chi_sim'] if command else
                        [windows_ocr, '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File',
                         str(self.root / 'scripts/privacy-ocr.ps1'), '-ImagePath', str(image)])
                result = subprocess.run(args, capture_output=True, timeout=45)
                if result.returncode:
                    self.add('ocr-failed-use-manual-review', source)
                else:
                    self.ocr_contents(decode(result.stdout), source, path)
            except (OSError, subprocess.TimeoutExpired, UnicodeError):
                self.add('ocr-failed-use-manual-review', source)

    def ocr_contents(self, text: str, source: str, path: str):
        self.text(text, source + '!OCR', path)
        # OCR may split a name into multiple words/characters. This is only an
        # additional signal; unknown names and failed recognition still need review.
        words = re.findall(r'\w+', unicodedata.normalize('NFKC', text))
        for length in (2, 3, 4):
            if any(digest(''.join(words[i:i + length])) in self.policy['blocked_token_sha256']
                   for i in range(len(words) - length + 1)):
                self.add('blocked-personal-identifier', source + '!OCR')

    def file(self, path: str, oid: str, mode='100644', source='', data: bytes | None = None, inspect=False):
        if (path, oid, mode) in self.seen:
            return
        self.seen.add((path, oid, mode))
        self.scanned += 1
        source = source or path
        self.text(path, source + '!filename')
        if any(fnmatch.fnmatchcase(path.lower(), pattern.lower()) for pattern in self.policy['forbidden_paths']):
            self.add('forbidden-file', source)
        if mode in {'120000', '160000'}:
            self.add('unreviewable-link-or-submodule', source)
            return
        data = self.object(oid) if data is None else data
        if data is None or len(data) > MAX_BYTES:
            self.add('file-size-limit', source)
            return
        suffix = Path(path).suffix.lower()
        binary = suffix in BINARY or (b'\0' in data[:8192] and not data.startswith((b'\xff\xfe', b'\xfe\xff')))
        if data.startswith(b'version https://git-lfs.github.com/spec/v1'):
            self.add('lfs-object-not-inspected', source)
            return
        if suffix in {'.docx', '.xlsx', '.pptx'}:
            self.document(data, source, path)
        if binary:
            approved = any(a['path'] == path and a['sha256'] == hashlib.sha256(data).hexdigest() for a in self.policy['reviewed_assets'])
            if not approved:
                self.add('binary-review-required', source)
            # Inspect mode helps reviewers; normal hooks never infer approval from OCR.
            if inspect and not approved and suffix in IMAGE:
                self.image_text(data, source, path)
            return
        try:
            self.text(decode(data), source, path)
        except UnicodeError:
            self.add('text-encoding-unreadable', source)

    def staged(self):
        self.current_identity()
        changed = set(git(self.root, 'diff', '--cached', '--name-only', '-z', '--diff-filter=ACMRTUXB').split(b'\0'))
        for entry in git(self.root, 'ls-files', '--stage', '-z').split(b'\0'):
            if not entry:
                continue
            meta, rawpath = entry.split(b'\t', 1)
            mode, oid, stage = meta.decode().split()
            path = rawpath.decode('utf8')
            if stage != '0':
                self.add('unmerged-index', path)
            elif rawpath in changed:
                self.file(path, oid, mode, 'index:' + path, inspect=True)

    def worktree(self, tracked_only=False):
        args = ['ls-files', '-z', '--cached']
        if not tracked_only:
            args += ['--others', '--exclude-standard']
        for rawpath in set(git(self.root, *args).split(b'\0')) - {b''}:
            path = rawpath.decode('utf8')
            file = self.root / path
            if not file.exists() and not file.is_symlink():
                continue
            if file.is_symlink():
                self.add('unreviewable-link-or-submodule', path)
                continue
            if not file.is_file():
                self.add('unreviewable-link-or-submodule', path)
                continue
            if file.stat().st_size > MAX_BYTES:
                self.add('file-size-limit', path)
                continue
            data = file.read_bytes()
            self.file(path, hashlib.sha256(data).hexdigest(), data=data)

    def history(self, tips: list[str]):
        if decode(git(self.root, 'rev-parse', '--is-shallow-repository')).strip() == 'true':
            raise GuardError('Full history required; fetch with --unshallow first')
        commits = set()
        for tip in tips:
            oid = decode(git(self.root, 'rev-parse', '--verify', '--end-of-options', tip)).strip()
            while decode(git(self.root, 'cat-file', '-t', oid)).strip() == 'tag':
                raw = decode(self.object(oid) or b'')
                self.text(raw, oid[:12] + ':tag')
                for line in raw.splitlines():
                    if line.startswith('tagger '):
                        self.identity(line[7:], oid[:12] + ':tagger')
                oid = raw.splitlines()[0].removeprefix('object ')
            if decode(git(self.root, 'cat-file', '-t', oid)).strip() != 'commit':
                raise GuardError('Only commit targets and annotated commit tags can be inspected')
            commits.update(decode(git(self.root, 'rev-list', oid)).splitlines())
        for commit in sorted(commits):
            data = self.object(commit)
            if data is None:
                raise GuardError('Commit metadata exceeds inspection limit')
            raw = decode(data)
            headers, message = raw.split('\n\n', 1)
            self.text(message, commit[:12] + ':message')
            for line in headers.splitlines():
                if line.startswith(('author ', 'committer ')):
                    kind, identity = line.split(' ', 1)
                    self.identity(identity, commit[:12] + ':' + kind)
            for entry in git(self.root, 'ls-tree', '-r', '-z', commit).split(b'\0'):
                if not entry:
                    continue
                meta, rawpath = entry.split(b'\t', 1)
                mode, _, oid = meta.decode().split()
                path = rawpath.decode('utf8')
                self.file(path, oid, mode, commit[:12] + ':' + path)

    def report(self) -> int:
        priority = {'secret-signature': 0, 'credential-literal': 1, 'credential-default': 1,
                    'url-credentials': 1, 'personal-email': 2, 'identity-use-private-email': 2,
                    'blocked-personal-identifier': 3, 'binary-review-required': 9}
        ordered = sorted(self.findings, key=lambda item: (priority.get(item[0], 5), item))
        for rule, source, line in ordered[:100]:
            print(f'BLOCK {rule}: {self.safe_source(source)}:{line}')
        if self.findings:
            print('By rule: ' + ', '.join(f'{rule}={count}' for rule, count in sorted(Counter(f[0] for f in self.findings).items())))
            print(f'Privacy check BLOCKED: {len(self.findings)} findings; {self.scanned} file versions inspected. Values omitted. Showing at most 100 locations.')
            print('Fix the staged content/history or explicitly review a binary. See docs/PRIVACY-GUARD.md.')
            return 1
        print(f'Privacy check passed: {self.scanned} file versions inspected (rule-based, not a guarantee).')
        return 0


def install(root: Path, name: str | None, email: str | None):
    policy = load_policy(root)
    guard = Guard(root, policy)
    try:
        if bool(name) != bool(email):
            raise GuardError('Supply both --name and --email, or neither')
        if name:
            if any(c in name + email for c in '\r\n<>'):
                raise GuardError('Invalid identity')
            guard.identity(f'{name} <{email}>', 'requested-identity')
            if guard.findings:
                raise GuardError('Use a public nickname and privacy-safe email')
        existing = decode(git(root, 'config', '--get', 'core.hooksPath', allow_failure=True)).strip()
        if existing and existing != '.githooks':
            raise GuardError('Existing core.hooksPath found; integrate existing hooks before installing')
        default_hooks = Path(decode(git(root, 'rev-parse', '--git-path', 'hooks')).strip())
        if not default_hooks.is_absolute():
            default_hooks = root / default_hooks
        if not existing and default_hooks.exists() and any(f.is_file() and not f.name.endswith('.sample') for f in default_hooks.iterdir()):
            raise GuardError('Existing hooks found; integrate them before installing')
        for hook in ('pre-commit', 'commit-msg', 'pre-push', 'pre-merge-commit', 'pre-applypatch', 'applypatch-msg', 'run-privacy'):
            file = root / '.githooks' / hook
            if not file.is_file():
                raise GuardError('Hook files missing')
            file.chmod(file.stat().st_mode | 0o111)
        git(root, 'config', '--local', 'core.hooksPath', '.githooks')
        if name:
            git(root, 'config', '--local', 'user.name', name)
            git(root, 'config', '--local', 'user.email', email)
        print('Installed privacy hooks for this clone. Re-run installation after cloning elsewhere.')
        guard.current_identity()
        if guard.findings:
            print('Current identity will be BLOCKED; configure a public nickname and privacy-safe email.')
    finally:
        guard.close()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['staged', 'worktree', 'history', 'pre-push', 'commit-msg', 'identity', 'install', 'inspect-asset'])
    parser.add_argument('targets', nargs='*')
    parser.add_argument('--tracked-only', action='store_true')
    parser.add_argument('--name')
    parser.add_argument('--email')
    args = parser.parse_args()
    root = Path(decode(git(Path.cwd(), 'rev-parse', '--show-toplevel')).strip())
    if args.mode == 'install':
        install(root, args.name, args.email)
        return 0
    # Delete-only pushes remove refs and do not introduce objects.
    tips = []
    if args.mode == 'pre-push':
        for line in sys.stdin:
            fields = line.split()
            if len(fields) != 4 or not all(re.fullmatch('[a-f0-9]{40}|[a-f0-9]{64}', fields[i]) for i in (1, 3)):
                raise GuardError('Malformed pre-push input')
            if set(fields[1]) != {'0'}:
                tips.append(fields[1])
        if not tips:
            print('No new objects to push.')
            return 0
    guard = Guard(root, load_policy(root, staged=args.mode in {'staged', 'commit-msg'}))
    try:
        if args.mode == 'staged':
            guard.staged()
        elif args.mode == 'identity':
            guard.current_identity()
        elif args.mode == 'worktree':
            guard.worktree(args.tracked_only)
        elif args.mode == 'commit-msg':
            if len(args.targets) != 1:
                raise GuardError('A commit message file is required')
            guard.current_identity()
            guard.text(decode(Path(args.targets[0]).read_bytes()), 'commit-message')
        elif args.mode in {'history', 'pre-push'}:
            if args.mode == 'history':
                tips = args.targets or decode(git(root, 'for-each-ref', '--format=%(objectname)')).splitlines()
                if not tips:
                    tips = ['HEAD']
            guard.history(tips)
        elif args.mode == 'inspect-asset':
            if len(args.targets) != 1:
                raise GuardError('One asset path is required')
            file = (root / args.targets[0]).resolve()
            if not file.is_relative_to(root.resolve()):
                raise GuardError('Asset must be inside the repository')
            if file.stat().st_size > MAX_BYTES:
                raise GuardError('Asset exceeds inspection limit')
            data = file.read_bytes()
            path = file.relative_to(root.resolve()).as_posix()
            fingerprint = hashlib.sha256(data).hexdigest()
            guard.file(path, fingerprint, data=data, inspect=True)
            print('Asset SHA-256: ' + fingerprint)
            print('Review pixels/metadata yourself. No approval was created.')
        return guard.report()
    finally:
        guard.close()


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except (GuardError, OSError, ValueError, KeyError, TypeError) as exc:
        # Exceptions can contain source bytes, filenames or credentials: never echo them.
        print('Privacy check BLOCKED: inspection/configuration error (' + type(exc).__name__ + '). Check Git, Python 3.11+, policy and complete history.', file=sys.stderr)
        raise SystemExit(2)
