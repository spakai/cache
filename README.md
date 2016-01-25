#Motivation

A reimaging by Brian Goetz's example of a efficient scalable cache for concurrent use in his book Java Concurrency In Practice.

#Current Issues
My unit test cases are failing intermittently because counters are being incremented asynchronously . Looks like one way to counter this is to use the method in Jeff Langr book to test multithreaded programs i.e condition variable. This should work as i am using counters to test if the value has been cached or otherwise.

