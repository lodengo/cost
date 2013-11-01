what's the problem?

in short, clients want operate (cost node CRUD) on cost file smoothly and got the file's "costs' fees" updated correct & timely.

what's cost file?

a cost file is a doc whose root is a node of cost.

what's cost node?

a cost node have one fees node and 0..* of child cost node.

what's fees?

fees consisted of 1..* fee which defines the feeName and feeExpr.

show me the Schema?
CostFile.xsd
===================================
this solution:
the basex server response to the clients' operation, and add the affected cost node(fees being dirty) to jobs repertory (redis).

the dispatcher fetch a job and dispatch it to a worker, loop.

the worker calculate the dirty node's fees and update it, job done, ready to work...

==================================
install:

1. install nodejs, see http://nodejs.org/
2. install basex, see http://basex.org/
3. install redis, see http://redis.io/

===============================
setup:

1. basex: see: http://docs.basex.org/wiki/Repository http://docs.basex.org/wiki/RESTXQ

repo install Fees.jar

repo install calcFunc.xqm

repo install calculator.xqm

create database fees fees.xml

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




