#!/usr/bin/env python3
"""Capture actual Android emulator screens; never treats them as message acceptance."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OUTPUT = Path('docs/validation/branding')
PACKAGE = 'net.lefil.fdroid'


def adb(*args, timeout=30):
    return subprocess.run(['adb', *args], check=True, capture_output=True,
                          timeout=timeout).stdout


def hierarchy():
    adb('shell', 'uiautomator', 'dump', '/sdcard/lefil-branding.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/lefil-branding.xml'))


def screenshot(name):
    data = adb('exec-out', 'screencap', '-p')
    if not data.startswith(b'\x89PNG\r\n\x1a\n'):
        raise RuntimeError('screencap did not return a PNG')
    (OUTPUT / name).write_bytes(data)


def capture_app(mode):
    adb('shell', 'cmd', 'uimode', 'night', 'yes' if mode == 'dark' else 'no')
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    deadline = time.monotonic() + 50
    last_error = None
    while time.monotonic() < deadline:
        try:
            tree = hierarchy()
            nodes = list(tree.iter('node'))
            logo = any(n.get('class') == 'android.widget.ImageView'
                       and n.get('content-desc') == 'Le Fil' for n in nodes)
            loading = any(n.get('resource-id') == PACKAGE + ':id/site_progress' for n in nodes)
            if logo and not loading:
                (OUTPUT / (mode + '-hierarchy.xml')).write_bytes(ET.tostring(tree))
                screenshot('app-' + mode + '.png')
                return
        except (subprocess.SubprocessError, ET.ParseError) as error:
            last_error = str(error)
        time.sleep(1)
    raise RuntimeError('Native logo not visible or page still loading in ' + mode + ': ' + str(last_error))


def capture_launcher():
    adb('shell', 'cmd', 'uimode', 'night', 'no')
    adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')
    dimensions = adb('shell', 'wm', 'size').decode()
    width, height = map(int, re.findall(r'(\d+)x(\d+)', dimensions)[-1])
    adb('shell', 'input', 'swipe', str(width // 2), str(height * 9 // 10),
        str(width // 2), str(height // 5), '500')
    tree = hierarchy()
    labels = [n for n in tree.iter('node') if n.get('text') == 'Le Fil']
    if not labels:
        raise RuntimeError('Le Fil not found in the actual launcher app drawer')
    screenshot('launcher-drawer.png')
    (OUTPUT / 'launcher-drawer-hierarchy.xml').write_bytes(ET.tostring(tree))
    x0, y0, x1, y1 = map(int, re.findall(r'\d+', labels[0].get('bounds')))
    adb('shell', 'input', 'draganddrop', str((x0 + x1) // 2), str((y0 + y1) // 2),
        str(width // 2), str(height // 2), '2000')
    adb('shell', 'input', 'keyevent', 'KEYCODE_HOME')
    time.sleep(2)
    tree = hierarchy()
    if not any(n.get('text') == 'Le Fil' for n in tree.iter('node')):
        raise RuntimeError('Home shortcut placement failed; no home-screen validation claimed')
    if any(n.get('resource-id', '').endswith(':id/apps_view') for n in tree.iter('node')):
        raise RuntimeError('Launcher still shows the drawer, not the home screen')
    screenshot('launcher-home.png')
    (OUTPUT / 'launcher-home-hierarchy.xml').write_bytes(ET.tostring(tree))


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name in ('native-test.txt', 'light-hierarchy.xml', 'dark-hierarchy.xml',
                 'launcher-drawer-hierarchy.xml', 'launcher-home-hierarchy.xml',
                 'app-light.png', 'app-dark.png', 'launcher-drawer.png',
                 'launcher-home.png', 'result.json'):
        (OUTPUT / name).unlink(missing_ok=True)
    hardware = adb('shell', 'getprop', 'ro.hardware').decode().strip()
    api = adb('shell', 'getprop', 'ro.build.version.sdk').decode().strip()
    if hardware not in ('ranchu', 'goldfish') or api != '35':
        raise RuntimeError('Only a disposable Android 15 emulator is supported')
    adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk', timeout=60)
    adb('install', '-r', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk', timeout=60)
    native = adb('shell', 'am', 'instrument', '-w', '-r',
                 PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner', timeout=180)
    (OUTPUT / 'native-test.txt').write_bytes(native)
    if not re.search(rb'OK \(1 test\)', native) or b'FAILURES!!!' in native:
        raise RuntimeError('Existing native renderer regression test did not pass')
    capture_app('light')
    capture_app('dark')
    capture_launcher()
    files = {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
             for p in OUTPUT.iterdir() if p.name != 'result.json'}
    result = {
        'source_commit': os.environ['GITHUB_SHA'],
        'run_url': 'https://github.com/' + os.environ['GITHUB_REPOSITORY']
                   + '/actions/runs/' + os.environ['GITHUB_RUN_ID'],
        'android_api': int(api), 'hardware': hardware,
        'native_renderer_regression': {'tests': 1, 'passed': 1, 'failed': 0},
        'captures': ['app-light.png', 'app-dark.png', 'launcher-drawer.png', 'launcher-home.png'],
        'file_sha256': files,
        'visual_review': 'pending human or assistant inspection of actual captures',
        'real_accounts_used': False, 'notifications_validated': False,
        'physical_phone_validated': False, 'published_fdroid_listing_validated': False,
    }
    (OUTPUT / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print('::notice::Android 15: existing native regression passed; launcher and day/night captures saved; notifications not validated')


if __name__ == '__main__':
    main()
