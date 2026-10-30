"""Real-network end-to-end smoke test using only Python's standard library.

Runs against the demo database. Uses fictional seeded accounts and advances an
unused service slot; it does not destroy data or reset existing bookings.
"""
import http.cookiejar
import json
import os
from pathlib import Path
import time
import uuid
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser

BASE_URL = os.environ.get('BASE_URL', 'http://127.0.0.1:18082').rstrip('/')
parsed = urllib.parse.urlparse(BASE_URL)
LOCAL = parsed.hostname in {'localhost', '127.0.0.1', '::1'} and parsed.scheme == 'http'
if not LOCAL and parsed.scheme != 'https':
    raise ValueError('Remote smoke requires HTTPS')

def credentials(key, local_user, local_password):
    if LOCAL:
        return (os.environ.get(key+'_USER', local_user), os.environ.get(key+'_PASSWORD', local_password))
    user, password = os.environ[key+'_USER'], os.environ[key+'_PASSWORD']
    if user in {'customer1','customer2','customer3','customer4','advisor'}:
        raise ValueError('Remote smoke must use private staging accounts')
    return user, password


class CsrfParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.value = None

    def handle_starttag(self, tag, attrs):
        if tag == 'input':
            a = dict(attrs)
            if a.get('name') == '_csrf':
                self.value = a.get('value')


class Session:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.csrf = None

    def request(self, method, path, data=None, form=False, expected=200):
        headers = {}
        body = None
        if data is not None:
            body = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
        if method not in {'GET', 'HEAD'} and self.csrf:
            headers['X-CSRF-TOKEN'] = self.csrf
        req = urllib.request.Request(BASE_URL + path, data=body, headers=headers, method=method)
        try:
            response = self.opener.open(req, timeout=20)
        except urllib.error.HTTPError as e:
            raw = e.read().decode('utf-8', 'replace')[:700]
            raise AssertionError(f'{method} {path}: HTTP {e.code}: {raw}') from e
        with response:
            status, raw = response.status, response.read().decode('utf-8', 'replace')
        assert status == expected, f'{method} {path}: expected {expected}, got {status}'
        return json.loads(raw) if 'application/json' in response.headers.get('Content-Type', '') else raw

    def login(self, username, password):
        login = self.request('GET', '/login')
        parser = CsrfParser()
        parser.feed(login)
        assert parser.value, 'CSRF token missing from actual rendered login form'
        # Spring Security redirects after a successful form login; urllib follows it.
        self.csrf = parser.value
        response = self.request('POST', '/login', {'username': username, 'password': password,
                                                   '_csrf': parser.value}, form=True)
        assert 'Incorrect username or password' not in response, f'Login failed: {username}'
        token = self.request('GET', '/api/csrf')
        self.csrf = token['token']
        assert isinstance(self.request('GET', '/api/motorcycles'), list)
        return self


def main():
    for attempt in range(60):
        try:
            with urllib.request.urlopen(BASE_URL + '/login', timeout=3) as response:
                if response.status == 200:
                    break
        except (OSError, urllib.error.URLError):
            time.sleep(2)
    else:
        raise RuntimeError('App did not serve /login within 120 seconds')

    for probe in ('liveness', 'readiness'):
        assert Session().request('GET', '/actuator/health/'+probe) == {'status': 'UP'}
    if os.environ.get('SMOKE_PHASE') == 'persistence':
        receipt = json.loads(Path(os.environ['SMOKE_RESULT_PATH']).read_text())
        customer = Session().login(*credentials('SMOKE_C3', 'customer3', 'DemoRide21!'))
        assert any(b['id'] == receipt['waitlist_promoted_to'] and b['status'] == 'COMPLETED'
                   and len(b['history']) == 3 for b in customer.request('GET', '/api/bookings'))
        print('PASS: completed promoted booking and history survived app restart')
        return
    c1 = Session().login(*credentials('SMOKE_C1', 'customer1', 'DemoRide21!'))
    c2 = Session().login(*credentials('SMOKE_C2', 'customer2', 'DemoRide21!'))
    c3 = Session().login(*credentials('SMOKE_C3', 'customer3', 'DemoRide21!'))
    advisor = Session().login(*credentials('SMOKE_ADVISOR', 'advisor', 'DemoAdvisor21!'))
    # Verify the real HTTP path still enforces authorization and CSRF.
    for session, method, path in ((c1, 'GET', '/api/advisor/bookings'),
                                  (Session(), 'POST', '/api/bookings')):
        try:
            session.request(method, path)
        except AssertionError as exc:
            assert 'HTTP 403' in str(exc), str(exc)
        else:
            raise AssertionError('Expected HTTP 403 for '+path)
    registration = 'SMK'  + uuid.uuid4().hex[:9].upper()
    bike_new = c1.request('POST', '/api/motorcycles',
                          {'registration': registration, 'model': 'Fictional Roadster',
                           'note': 'Synthetic end-to-end registration'}, expected=201)
    assert bike_new['registration'] == registration
    bikes = [bike_new['id'], c2.request('GET', '/api/motorcycles')[0]['id'],
             c3.request('GET', '/api/motorcycles')[0]['id']]
    slots = c1.request('GET', '/api/slots')
    slots = [s for s in slots if s['available'] >= 2 and s['capacity'] == 2]
    assert slots, 'No empty capacity-2 slot; use a fresh demo database for repeated runs'
    slot_id = slots[0]['id']

    def payload(bike):
        return {'motorcycleId': bike, 'slotId': slot_id,
                'serviceType': 'REGULAR_SERVICE', 'note': 'Automated synthetic smoke'}

    a = c1.request('POST', '/api/bookings', payload(bikes[0]), expected=201)
    b = c2.request('POST', '/api/bookings', payload(bikes[1]), expected=201)
    assert a['status'] == b['status'] == 'CONFIRMED'
    w = c3.request('POST', '/api/waitlist', payload(bikes[2]), expected=201)
    assert w['status'] == 'WAITING'
    assert c1.request('GET', f'/api/slots/{slot_id}')['available'] == 0
    c1.request('POST', f"/api/bookings/{a['id']}/cancel")
    waitlist = c3.request('GET', '/api/waitlist')
    promoted = next(x for x in waitlist if x['id'] == w['id'])
    assert promoted['status'] == 'PROMOTED' and promoted['bookingId'], promoted
    promoted_id = promoted['bookingId']
    advisor.request('PATCH', f'/api/advisor/bookings/{promoted_id}/status', {'status': 'IN_SERVICE'})
    done = advisor.request('PATCH', f'/api/advisor/bookings/{promoted_id}/status',
                           {'status': 'COMPLETED'})
    assert done['status'] == 'COMPLETED' and len(done['history']) == 3, done
    customer_history = c3.request('GET', '/api/bookings')
    assert any(x['id'] == promoted_id and x['status'] == 'COMPLETED'
               and len(x['history']) == 3 for x in customer_history), customer_history
    occupancy = c2.request('GET', f'/api/slots/{slot_id}')
    assert occupancy['available'] == 0, occupancy
    results = {'status': 'PASS', 'kind': 'REAL_HTTP_POSTGRESQL_SMOKE',
               'registered_motorcycle': registration, 'slot': slot_id, 'capacity': 2, 'confirmed': 2,
               'waitlist_promoted_to': promoted_id, 'advisor_status': 'COMPLETED',
               'service_history_events': len(done['history'])}
    output = os.environ.get('SMOKE_RESULT_PATH')
    if output:
        Path(output).write_text(json.dumps(results, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(results, indent=2))


if __name__ == '__main__':
    main()
