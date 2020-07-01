# clojure-queues

Clojure web server that exposes an API that interacts with a **job queue** system.

## Description

We have a large Customer Experience team, focusing on delivering high quality, friendly support to our
customers. To ensure the efficiency and success of this operation, we organize the work to be done in job queues.

In this exercise you're tasked with developing a simplified version of a job queue. The important entities in this
domain are *jobs* and *agents*.

A *job* is any task that needs to get done. It has a unique id, a type - which denotes the category of that job -, and
an urgency (boolean) flag - indicating whether that job has a high priority.

An *agent* is someone that performs a job. They also have a unique id and two disjoint skill sets: primary and
secondary. Skill sets are a simple list of job types that an agent is allowed to perform.

The core operation of a job queue is the dequeue function, which, given a pool of jobs to be done and agent's job
request, and a set of priority rules, returns the fittest job to be performed by that agent. Your first task is to
implement a dequeue function that abides to these rules:

- You can assume the list of jobs passed in is ordered by the time the they have entered the system.
- Jobs that arrived first should be assigned first, unless it has a "urgent" flag, in which case it has a higher
  priority.
- A job cannot be assigned to more than one agent at a time.
- An agent is not handed a job whose type is not among its skill sets.
- An agent only receives a job whose type is contained among its secondary skill set if no job from its primary
  skill set is available.

A job is considered to be done when the agent it was assigned to requests a new job.

Attached are two files: `sample-input.json` and `sample-output.json`. Your program should be able to take the
contents of the `sample-input.json` file via stdin and produce the contents of `sample-output.json` on stdout.

As we rely heavily on automated tests and our CI tool to ship code to production multiple times per day, 
having tests that make sure your code works is a must.

Also, pay attention to code organization and make sure it is readable and clean.

#### License CC0 1.0 (Public Domain)
