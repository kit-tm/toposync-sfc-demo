import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation
import csv
from collections import deque

WINDOW_SIZE = 100

fig, ax = plt.subplots()

def animate(i):
    xs = deque(maxlen=WINDOW_SIZE)
    ys_10 = deque(maxlen=WINDOW_SIZE)
    ys_11 = deque(maxlen=WINDOW_SIZE)

    f = open('ping_data.csv', 'r')
    reader = csv.reader(f, delimiter=',')
    for row in reader:
        xs.append(int(row[0]))
        ys_10.append(int(row[1]))
        ys_11.append(int(row[2]))

    line1, = plt.plot(xs, ys_10, 'r')
    line2, = plt.plot(xs, ys_11, 'b')

    ax.set_xlim(xs[0], xs[len(xs)-1])

    max_y = max(max(ys_10), max(ys_11))
    top = plt.ylim()[1]
    if max_y > top:
        plt.ylim(top=max_y)

    plt.legend((line1,line2), ('client1 (10.0.0.10)', 'client2 (10.0.0.11)'))

ani = FuncAnimation(fig,animate, 1000)
ax.set_ylim((0, 1000))
plt.title('Ping Round-Trip Times')
plt.xlabel('ping index')
plt.ylabel('RTT [ms]')
fig.canvas.set_window_title('TopoSync-SFC Demo Liveplot')
plt.show()