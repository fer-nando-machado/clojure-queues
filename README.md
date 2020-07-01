# clojure-queues

Clojure web server that exposes an API that interacts with a **job queue** system.

## Problem

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

This system proposes a solution to solve a job queue problem. It was designed to run as a web server
and the user is able to interact with its data through a HTTP API that accepts and returns JSON payloads.

The solution keeps track of two main entities: agents and jobs. Using the exposed API methods, users
are able to add entries to these entities and also query customized information such as how many jobs a
certain agent has performed or view the current state of the job queue sorted by status.

In order to make processing less costly, jobs are kept in three different collections: jobs that have
been finished, jobs that are currently assigned and jobs that are waiting to be assigned. This was done
because moving a job from one collection to another appears to be simpler than querying the collection to
update a job’s status every time a status change happens. Another benefit from this approach is that jobs
don’t have to be filtered when a job request is made or when an user wants to view the state of the queue.

Using the request method, the user is able to dequeue jobs and assign them to an agent. Once a
request is made, the system tries to find the best fitting job for the agent that requested it. If a job is found,
it gets assigned to the agent and moved from the open jobs to the active jobs collection. If the requesting
agent was already working a job, then that job is moved from the active jobs to the finished jobs collection.

This document also covers instructions for starting the web server and running the test suite.

## Commands

The following commands require [Lein-Ring](https://clojars.org/lein-ring) installed.

### Test 
#### `lein midje :autotest`

Runs the tests suite. The `:autotest` parameters makes it look for changes and run the tests again on updated files.

### Build
#### `lein ring uberjar`

Builds an standalone executable `.jar` file. This file can be executed by running `java -jar file.jar`.

### Run
#### `lein ring server-headless`

Runs the server and exposes the API on port `3000`.

## Endpoints

Once the server is up and running, a variety of endpoints can be accessed to interact with the system.
On the section below, you can find a list of all available HTTP API methods along with some examples
of the accepted and returned JSON payloads for each one of them.

#### `GET /agents` 
Returns an array of all agents.
###### response 
    [{"id":"8ab86c18", "name":"BoJack Horseman", "primary_skillset":["bills"], "secondary_skillset":[]},
     {"id":"ed0e23ef", "name":"Mr. Peanut Butter", "primary_skillset":["rewards"],  "secondary_skillset":["bills"]}]

#### `POST /agents/add` 
Adds the agent informed on the request the agent collection. Returns an array of all agents.
###### request 
    {"id": "8ab86c18", "name": "BoJack Horseman", "primary_skillset": ["bills-questions"], "secondary_skillset": []}
###### response 
    [{"id":"8ab86c18", "name":"BoJack Horseman", "primary_skillset":["bills"], "secondary_skillset":[]}]

#### `POST /agents/state` 
Given an agent id, returns how many jobs of each type this agent has performed. (**TO-DO:** *this should be a GET method instead of POST*)
###### request 
    {"id": "ed0e23ef"}
###### response 
    {"rewards":1, "bills":1} 

#### `GET /jobs` 
Returns an object containing a breakdown of the all jobs, including finished jobs, active jobs and jobs that are still waiting on the queue.
###### response 
    {
        "finished":[],
        "active": [{"id":"c0033410", "type":"bills", "urgent":true, "agent_id":"8ab86c18"},
                   {"id":"f26e890b", "type":"rewards", "urgent":false, "agent_id":"ed0e23ef"}],
        "waiting":[{"id":"690de6bc", "type":"bills", "urgent":false}]
    }
    
#### `POST /jobs/add` 
Adds the informed job the job queue. Returns an array of the jobs on the queue.
###### request 
    {"id": "f26e890b", "type": "rewards_question", "urgent": false}
###### response 
    [{"id": "f26e890b", "type": "rewards_question", "urgent": false}]

#### `POST /jobs/request` 
Processes a job request by the agent informed. If a job is found, responds with its id. (**TO-DO:** *are both fields necessary in response? if a job is not found, what is returned?*)

###### request 
    {"agent_id": "ed0e23ef"} 
###### response 
    {"agent_id":"ed0e23ef", "job_id":"f26e890b"}

## Explanation

The main program consists of five distinct steps, which will be further explained below.

#### `(defn -main [& args] (array-to-json (dequeue (array-to-map (json-to-array (read-line))))))`

#### 1. `(read-line)`

This function is called to read the JSON string from *in*.

#### 2. `(json-to-array json)`

This function is used to treat and convert the input JSON to a data array.

The JSON keys had to be trimmed to remove unwanted additional spaces and, later,
converted to special keywords so they can be easily referenced on the program.

The JSON values also need to be treated because, on the input example, slightly different
strings could be found as skills. To solve this, a regex is applied to associate them with keywords.

#### 3. `(array-to-map array)`

This function is used to group data from the input array into a map. Here, each item of the
array is grouped into sub-arrays that are easily identified by a keyword: agents, jobs and requests,
while still following the order they appeared on the original input JSON.

#### 4. `(dequeue data)`

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

#### 5. `(array-to-json array)`

This function takes the assignments array created on the previous step and returns a JSON
string formatted with pretty-print.

## Feedback

- We think you could have designed the solution in a way that better separates pure logic from impure code
- There's a concurrency vulnerability in the code that updates the atom: there's a read operation immediately before the swap! and the result of this read operation has influence over whether swap! will execute or not; reading and then wapping os not an atomic operation, so a better solution would be to make that decision inside the swap function.
- A single namespace solution was fine for the first half of the exercise, but when the codebase grows, it's wise to explode a single namespace into many: it improves code and tests readability.
- This is not a huge issue - and did directly influence our decision - but it's something we noticed: the code contains excessive documentation. Function documentation is ok but can often be more terse or even absent (if the function name is clear enough). We also thought it was a little out of place having it written in Portuguese, since the code was written in English.

## License
2018 [MIT License](LICENSE)
