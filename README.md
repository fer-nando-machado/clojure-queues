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

## Solution

The main program consists of five distinct steps, which will be further explained below.

`(defn -main [& args] (array-to-json (dequeue (array-to-map (json-to-array (read-line))))))`

### 1. `(read-line)`

This function is called to read the JSON string from *in*.

### 2. `(json-to-array json)`

This function is used to treat and convert the input JSON to a data array.
The JSON keys had to be trimmed to remove unwanted additional spaces and, later,
converted to special keywords so they can be easily referenced on the program.
The JSON values also need to be treated because, on the input example, slightly different
strings could be found as skills. To solve this, a regex is applied to associate them with keywords.

### 3. `(array-to-map array)`

This function is used to group data from the input array into a map. Here, each item of the
array is grouped into sub-arrays that are easily identified by a keyword: agents, jobs and requests,
while still following the order they appeared on the original input JSON.

### 4. `(dequeue data)`

This is the function where the main processing occurs. It takes as an argument a map
containing agents, jobs and requests and returns an array containing assignments.
Here, the requests are iterated so a best fitting task can be found for each one of them.
Before starting the iteration, the input jobs are sorted by their urgency. First, they are first
split into two new arrays containing the urgent and the non-urgent jobs, respectively, while still
following the order they appear on the original array. Then, these two arrays are concatenated to
create a new array where the urgent jobs appear first and the non-urgent appear last.
For each request, its related agent is first located using the (find-by-id coll id) function. This
is done because their skillset data is needed on the next step.
Using the (find-fittest-job-by-skillsets jobs agent) function, we try to locate the fittest job
for each agent, respecting their skillsets. Here, the jobs array is filtered using the
(filter-jobs-by-skillset jobs skillset) twice, first returning the jobs that match the agent’s primary
skillset and, then, the jobs that match its secondary skillset. The results of these two filter
operations are then concatenated into a new array and the first element of this array is returned as
the fittest job. This whole operation is lazy, so it is interrupted when the first matching job is found.
If a matching job is found during the previous step, an assignment is created to associate it
with the agent that made the request using the (assign-job job agent) function. This assigned job is
also removed from jobs array before iterating the next request.
Finally, when all requests are iterated, an array containing the assignments is returned.

### 5. `(array-to-json array)`

This function takes the assignments array created on the previous step and returns a JSON
string formatted with pretty-print.

## License
CC0 1.0 (Public Domain)
