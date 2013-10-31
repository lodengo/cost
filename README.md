what's the problem?

in short, clients want operate (cost unit node CRUD) on "cost file" smoothly and got the file's "fees" updated timely & correct.

what's "cost file"?

a cost file consisted of one top "cost unit" which may have 0..* of child "cost unit".

what's "cost unit"?

a cost unit have one "fees" and 0..* of child "cost unit".

what's "fees"?

fees consisted of 1..* fee which defines the fee name and calculate expr.
===================================
solution
the server response to the clients' operation, and add the affected cost-unit node(fees being dirty) to jobs pool.

the dispatcher fetch a job and dispatch it to a worker, loop.

the worker calculate the dirty node's fees and update it, job done, ready to work...

==================================
install:

1. install nodejs, see http://nodejs.org/
2. install basex, see http://basex.org/

===============================
setup:

1. basex: see: http://docs.basex.org/wiki/Repository http://docs.basex.org/wiki/RESTXQ

repo install Fees.jar

repo install calcFunc.xqm

repo install calculator.xqm

create database template template.xml

=========================

start:

1.run basex web application, see http://docs.basex.org/wiki/Web_Application

basexhttp

2.run dispatcher 

npm install 

node dispatcher

3.run test

copy test.html to basex's webapp/static folder,
open a browser, visit http://localhost:8984/static/test.html




