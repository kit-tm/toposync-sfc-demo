import matplotlib as mpl
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import csv
from collections import deque
import os
from matplotlib.ticker import MaxNLocator

DIR_NAME = os.path.dirname(__file__)
CSV_PATH = None
if DIR_NAME == '':
    CSV_PATH = 'ping_data.csv'
else:
    CSV_PATH = '%s/ping_data.csv' % DIR_NAME

WINDOW_SIZE = 100

mpl.rcParams['toolbar'] = 'None'
fig, ax = plt.subplots()
ax.get_xaxis().set_major_locator(MaxNLocator(integer=True))
last_line = 0

def animate(i):
    xs = deque(maxlen=WINDOW_SIZE)
    ys_10 = deque(maxlen=WINDOW_SIZE)
    ys_11 = deque(maxlen=WINDOW_SIZE)

    f = open(CSV_PATH, 'r')

    lines = f.readlines()

    for row in lines[-WINDOW_SIZE:]:
        splitted = row.split(',')
        xs.append(int(splitted[0]))
        ys_10.append(int(splitted[1]))
        ys_11.append(int(splitted[2]))

    line1, = plt.plot(xs, ys_10, 'r')
    line2, = plt.plot(xs, ys_11, 'b')

    if len(xs) > 1:
        ax.set_xlim(xs[0], xs[len(xs)-1])
        max_y = max(max(ys_10), max(ys_11))
        top = plt.ylim()[1]
        if max_y > top:
            plt.ylim(top=max_y)

    plt.legend((line1,line2), ('client1 (10.0.0.10)', 'client2 (10.0.0.11)'))

def plot():
    ani = FuncAnimation(fig,animate, 1000)
    ax.set_ylim((0, 1000))
    plt.title('Ping Round-Trip Times')
    plt.xlabel('ping index')
    plt.ylabel('RTT [ms]')
    fig.canvas.set_window_title('TopoSync-SFC Demo Liveplot')
    plt.show()

if __name__ == "__main__":
    plot()