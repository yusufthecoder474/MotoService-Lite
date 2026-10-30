"""Focused configuration and accidental-secret checks, not a full security audit."""
from pathlib import Path
import re
import subprocess
import yaml
root = Path(__file__).resolve().parents[1]
for path in list((root/'deploy/k8s').glob('*.yaml')) + list((root/'.github/workflows').glob('*.yml')):
    list(yaml.safe_load_all(path.read_text()))
app=list(yaml.safe_load_all((root/'deploy/k8s/app.yaml').read_text()))[0]
container=app['spec']['template']['spec']['containers'][0]
assert not container['image'].endswith(':latest')
assert container['securityContext']['readOnlyRootFilesystem']
assert container['readinessProbe']['httpGet']['path'].endswith('/readiness')
assert container['livenessProbe']['httpGet']['path'].endswith('/liveness')
assert app['spec']['template']['spec']['securityContext']['runAsNonRoot']
cloud=yaml.safe_load((root/'src/main/resources/application-cloud.yml').read_text())
assert cloud['app']['demo'] is False
assert cloud['spring']['datasource']['password'] == '${DB_PASSWORD}'
tracked=subprocess.check_output(['git','ls-files','-co','--exclude-standard'],cwd=root,text=True).splitlines()
patterns=[r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', r'gh[pousr]_[A-Za-z0-9]{30,}', r'AKIA[0-9A-Z]{16}']
for name in set(tracked):
    path=root/name
    if not path.is_file() or path.suffix in {'.png','.jpg','.zip'}: continue
    text=path.read_text(errors='replace')
    for pattern in patterns:
        assert not re.search(pattern,text), 'Possible secret in '+name
    assert not name.endswith(('.tfstate','.tfplan','.secret.yaml')), name
print('PASS: YAML, probe/security invariants, and limited credential-pattern scan')
