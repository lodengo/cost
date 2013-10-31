var poolModule = require('generic-pool');
var basex = require('simple-basex'); 
var redis = require("redis");

var workers = [{host:'localhost', port:1984, username:'admin', password:'admin'}];

var Worker = basex.Session;

var workersPool = poolModule.Pool({
    name: 'workers',
    create: function (callback) {
        var who = workers.shift();
        var worker = new Worker(who);
        callback(null, worker);
        workers.push(who);
    },
    destroy: function (worker) {
        worker.execute('exit');
    },
    max: 1024    
});

var redisPool = poolModule.Pool({
    name: 'redis',
    create: function (callback) {
    	var client = redis.createClient();
    	callback(null, client);
    },
    destroy: function (client) {
    	client.quit();
    },
    max: 1024    
});

function array_unique(arr) {
	var a = [];
    var l = arr.length;
    for(var i=0; i<l; i++) {
      for(var j=i+1; j<l; j++) {
        // If this[i] is found later in the array
        if (arr[i] === arr[j])
          j = ++i;
      }
      a.push(arr[i]);
    }
    return a;
};

Worker.prototype.doJob = function(job, callback){
	var me = this;
	me.jobTasks(job, function(err, tasks, score){
		if(tasks.length){
			var cmd = "xquery import module namespace c = 'cost.cloud.calculator'; c:calcs('"+job+"', '"+tasks.join(",")+"')";
			me.execute(cmd, function(err, res){
				if(res){
					var newTasks = array_unique(res.result.split(' '));
					
					callback(job, tasks, score, newTasks);
				}else{
					callback(job, tasks, score, []);
				}
			});
		}else{
			callback(job, [], score, []);
		}
	});
};

Worker.prototype.jobTasks = function(job, callback){
	redisPool.acquire(function(err, redisClient){
		redisClient.ZREVRANGE(job, 0, 0, "withscores", function(err, res){
			if(res.length == 2){
				var depth = res[1];
				redisClient.ZRANGEBYSCORE(job, depth, depth, function(err, tasks){
					redisPool.release(redisClient);
					callback(err, tasks, depth);
				});
			}else{
				redisPool.release(redisClient);
				callback(null, [], 0);				
			}
		});
	});
}

function Dispatcher(){
	
}

Dispatcher.prototype.fetchJob = function(callback){
	//var me = this;
	redisPool.acquire(function(err, redisClient){
		redisClient.sdiff("todo-jobs", "doing-jobs", function(err, dbs){
			if(dbs.length){
				var db = dbs[Math.floor(Math.random() * dbs.length)];
				redisClient.sadd("doing-jobs", db, function(err, res){
					redisPool.release(redisClient);					
					callback(err, db);
				});
			}else{
				redisPool.release(redisClient);
				callback(null, null);
			}			
		});
	});
};

Dispatcher.prototype.jobDone = function(doneJob, doneTasks, score, newTasks){
	var me = this;
	me.removeDoneJOb(doneJob, doneTasks, function(err, res){
		if(newTasks.length){
			newTasks.forEach(function(task){
				me.addJob(doneJob, task, score-1)
			});
		}
	});
}

Dispatcher.prototype.addJob = function(job, task, score){
	redisPool.acquire(function(err, redisClient){
		redisClient.zscore(job, task, function(err, res){
			if(null === res){
				redisClient.zadd(job, score, task, function(err,res){
					redisPool.release(redisClient);
				});
			}else{
				redisPool.release(redisClient);
			}				
		});
	});
}

Dispatcher.prototype.removeDoneJOb = function(doneJob, doneTasks, callback){
	redisPool.acquire(function(err, redisClient){
		doneTasks.unshift(doneJob);
		redisClient.zrem(doneTasks, function(err, res){ 
			redisClient.srem("doing-jobs", doneJob, function(err,res){
				redisClient.zcard(doneJob, function(err, res){
					if(res == 0){
						redisClient.del(doneJob, function(err, res){
							redisClient.srem("todo-jobs", doneJob, function(err, res){
								redisPool.release(redisClient);
								callback(null, null);
							});
						});						
					}else{
						redisPool.release(redisClient);
						callback(null, null);
					}
				});
			});
		});		
	});
}

Dispatcher.prototype.dispatch = function(){
	var me = this;
	me.fetchJob(function(err, job){		
		//process.nextTick(function(){me.dispatch();});
		//setTimeout(function(){me.dispatch();}, 2000);
		console.log('fetch job db:'+job);
		if(job){
			workersPool.acquire(function(err, worker){
				worker.doJob(job, function(doneJob, doneTasks, score, newTasks){
					console.log(['job done:', doneJob, doneTasks, score, newTasks]);
					workersPool.release(worker);
					me.jobDone(doneJob, doneTasks, score, newTasks);					
				});
			});
		}
	});
};

var dispatcher = new Dispatcher();
setInterval(function(){dispatcher.dispatch();}, 100);
//dispatcher.dispatch();


	

