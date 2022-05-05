from threading import Lock

class DocPoolCounter():
    def __init__(self):
        self._lock = Lock()
        self._count = 0
    
    def get_and_inc(self):
        with self._lock:
            current = self._count
            self._count += 1
            return current

    def reset_counter(self):
        with self._lock:
            self._counter = 0
