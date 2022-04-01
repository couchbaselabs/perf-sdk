package perf

import (
	"sync"
)

type docPoolCounter struct {
	mu    sync.Mutex
	count int
}

func (c *docPoolCounter) getAndInc() int {
	c.mu.Lock()
	current := c.count
	c.count += 1
	c.mu.Unlock()
	return current
}

func (c *docPoolCounter) resetCounter() {
	c.mu.Lock()
	c.count = 0
	c.mu.Unlock()
}
