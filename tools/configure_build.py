from pathlib import Path
import argparse,json,zipfile
p=argparse.ArgumentParser();p.add_argument('--instance',required=True,type=Path);p.add_argument('--libraries',required=True,type=Path);a=p.parse_args()
root=Path(__file__).resolve().parents[1];instance=a.instance.resolve();libraries=a.libraries.resolve();version=json.loads((instance/(instance.name+'.json')).read_text(encoding='utf-8-sig'))
cp=[instance/(instance.name+'.jar')]
def artifact(name):
 g,n,v,*classifier=name.split(':');return libraries/g.replace('.','/')/n/v/(n+'-'+v+('-'+classifier[0] if classifier else '')+'.jar')
for lib in version['libraries']:
 entry=lib.get('downloads',{}).get('artifact',{}).get('path');f=libraries/entry if entry else artifact(lib['name'])
 if f.exists():cp.append(f)
loaders=[f for f in cp if f.name.startswith('fabric-loader-')];assert len(loaders)==1,'Install Fabric Loader in this instance first'
with zipfile.ZipFile(loaders[0]) as z:
 for group in ['common','development']:
  for lib in json.loads(z.read('fabric-installer.json'))['libraries'].get(group,[]):
   f=artifact(lib['name'])
   if f.exists() and f not in cp:cp.append(f)
mods={}
for f in (instance/'mods').glob('*.jar'):
 with zipfile.ZipFile(f) as z:
  if 'fabric.mod.json' in z.namelist():mods[json.loads(z.read('fabric.mod.json'),strict=False)['id']]=f
assert 'fabric-api' in mods and 'appleskin' in mods,'Install Fabric API and AppleSkin for compilation'
dest=root/'.local';dest.mkdir(exist_ok=True)
for jar in [mods['fabric-api'],loaders[0]]:
 with zipfile.ZipFile(jar) as z:
  for n in z.namelist():
   if n.startswith('META-INF/jars/') and n.endswith('.jar'):
    f=dest/Path(n).name;f.write_bytes(z.read(n));cp.append(f)
cp.append(mods['appleskin']);assert all(f.exists() for f in cp)
(dest/'classpath.json').write_text(json.dumps([str(f) for f in dict.fromkeys(cp)],indent=2),encoding='utf-8');print('Configured local compilation dependencies.')
