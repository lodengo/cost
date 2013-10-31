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
1. run basex web application, see http://docs.basex.org/wiki/Web_Application
basexhttp

2. run dispatcher
npm install 
node dispatcher

3. run test
copy test.html to basex's webapp/static folder
open a browser, visit http://localhost:8984/static/test.html




