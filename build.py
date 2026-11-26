"""Build with Android SDK 34, JDK 17, and Python 3 on macOS or Linux."""
import xml.etree.ElementTree as ET
import os
import shutil
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parent
src = root / 'source'
sdk = Path(os.environ['ANDROID_HOME'])
tools = sdk / 'build-tools/34.0.0'
android = sdk / 'platforms/android-34/android.jar'
java = Path(os.environ['JAVA_HOME']) / 'bin'
out = root / 'dist'
out.mkdir(exist_ok=True)

def run(*args):
    subprocess.run(list(map(str, args)), check=True)

def build(work, app_only):
    for folder in ['gen', 'classes', 'dex']:
        (work / folder).mkdir()
    base = work / 'base.apk'
    manifest = src/'AndroidManifest.xml'
    rename_args = []
    filename = 'LivingRoomTV.apk'
    if app_only:
        tree = ET.parse(manifest)
        namespace = '{http://schemas.android.com/apk/res/android}'
        activity = tree.find('application/activity')
        for intent_filter in activity.findall('intent-filter'):
            categories = [c.get(namespace+'name') for c in intent_filter.findall('category')]
            if 'android.intent.category.HOME' in categories:
                activity.remove(intent_filter)
        tree.find('application').set(namespace+'label', '客厅应用')
        manifest = work/'AndroidManifest.xml'
        tree.write(manifest, encoding='utf-8')
        rename_args = ['--rename-manifest-package', 'org.livingroom.apps']
        filename = 'LivingRoomTV-App.apk'
    run(tools/'aapt', 'package', '-f', '-m', '-J', work/'gen', '-M', manifest, *rename_args,
        '-S', src/'res', '-I', android, '-F', base)
    run(java/'javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-classpath', android,
        '-d', work/'classes', *src.rglob('*.java'), *(work/'gen').rglob('*.java'))
    run(tools/'d8', '--lib', android, '--min-api', '21', '--output', work/'dex', *(work/'classes').rglob('*.class'))
    with ZipFile(base, 'a') as archive:
        archive.write(work/'dex/classes.dex', 'classes.dex')
    run(tools/'zipalign', '-f', '4', base, work/'aligned.apk')
    apk = work/'LivingRoomTV.apk'
    run(tools/'apksigner', 'sign', '--ks', os.environ['TV_KEYSTORE'], '--ks-pass', 'env:TV_STOREPASS',
        '--v4-signing-enabled', 'false', '--out', apk, work/'aligned.apk')
    run(tools/'apksigner', 'verify', '--verbose', apk)
    shutil.copyfile(apk, out/filename)

for app_only in (False, True):
    with tempfile.TemporaryDirectory() as tmp:
        build(Path(tmp), app_only)
