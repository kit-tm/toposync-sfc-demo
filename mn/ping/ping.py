import subprocess
import re

def run_command(command):
    p = subprocess.Popen(command,
                         stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT,
                         shell=True)
    return iter(p.stdout.readline, b'')

source_regex = r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}'
seq_regex = r'icmp_seq=\d*'
time_regex = r'time=\d*'



time_10 = None
time_11 = None

f = open('ping_data.csv', 'w').close()
f = open('ping_data.csv', 'a')

cnt = 0

command = 'ping -I shost-eth0 224.2.3.4'
for line in run_command(command):
    source = None
    seq = None

    match = re.search(source_regex, line)
    if match:
        source = match.group(0)

    match = re.search(seq_regex, line)
    if match:
        seq = match.group(0)[9:]

    match = re.search(time_regex, line)
    if match:
        if source == '10.0.0.10':
            time_10 = match.group(0)[5:]
        elif source == '10.0.0.11':
            time_11 = match.group(0)[5:]

    if time_10 is not None and time_11 is not None:
        data = '%s,%s,%s' % (seq, time_10, time_11)
        if cnt != 0:
            data = '\n' + data
        f.write(data)
        f.flush()
        time_10 = None
        time_11 = None
        cnt += 1
    

    

    

    


    