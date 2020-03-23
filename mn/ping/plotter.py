import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import csv
from collections import deque

WINDOW_SIZE = 100

xs = deque(maxlen=WINDOW_SIZE)
ys_10 = deque(maxlen=WINDOW_SIZE)
ys_11 = deque(maxlen=WINDOW_SIZE)

fig, ax = plt.subplots()

def animate(i):
    f = open('ping_data.csv', 'r')
    reader = csv.reader(f, delimiter=',')
    xs = []
    ys_10 = []
    ys_11 = []
    for row in reader:
        xs.append(int(row[0]))
        ys_10.append(int(row[1]))
        ys_11.append(int(row[2]))
        ax.set_xlim((len(xs)-WINDOW_SIZE, len(xs)))

    line1, = plt.plot(xs, ys_10, 'r')
    line2, = plt.plot(xs, ys_11, 'b')

    max_y = max(max(ys_10), max(ys_11))
    top = plt.ylim()[1]
    if max_y > top:
        plt.ylim(top=max_y + 100)

    plt.legend((line1,line2), ('client1', 'client2'))

ani = FuncAnimation(fig,animate, 1000)
ax.set_ylim((0, 1000))
plt.title('Ping Round-Trip Times')
plt.xlabel('ping index')
plt.ylabel('RTT [ms]')
fig.canvas.set_window_title('TopoSync-SFC Demo Liveplot')
plt.show()