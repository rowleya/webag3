#!/bin/bash
#
# Init file for WebAG3 (Jabber Proxy Server)
#
# chkconfig: 2345 55 25
# description: WebAG3 (Jabber Proxy Server)
#
# pidfile: /var/run/webag3.pid
. /etc/rc.d/init.d/functions
JAVA=/usr/bin/java
WEBAG3_JAR=/opt/webag3/webag3.jar
JAVA_OPTS=
WEBAG3_OPTS=
[ -f /etc/sysconfig/webag3 ] && . /etc/sysconfig/webag3
webag3dir=`dirname $WEBAG3_JAR`
[ -e $webag3dir/log4j.properties ] && JAVA_OPTS="$JAVA_OPTS -Dlog4j.configuration=file:$webag3dir/log4j.properties"
logfile=/var/log/webag3.log
pidfile=/var/run/webag3.pid
RETVAL=0

webag3status()
{
	local procpid=`pgrep -f "java .*webag3"`
	local pid=
	local tpid=
	[ -f $pidfile ] && read pid < $pidfile
	# pgrep -f might return its own pid in its output, find the real pid by looking at /proc
	for tpid in $procpid; do procpid=; [ -d /proc/$tpid ] && grep "webag3" /proc/$tpid/cmdline > /dev/null && procpid=$tpid && break; done
	if [ -n "$pid" ]; then
		if [ -n "$procpid" ]; then
			if [ "$pid" = "$procpid" ]; then
				echo $"webag3 (pid $pid) is running..."
				return 0
			else
				echo $"webag3 appears to be running but the process pid ($procpid) does not match the pid in $pidfile ($pid)"
				return 0
			fi
		else
			echo $"webag3 dead but pid file exists"
			return 1
		fi
	elif [ -n "$procpid" ]; then
		echo $"webag3 (pid $procpid) is running but no pid file exists"
		return 0
	fi
	echo $"webag3 is stopped"
	return 2
}

start()
{
	echo -n $"Starting webag3:"
	# check if it's still running and only attempt to start it if it's not
	# attempting to start it while it's running fails without returning
	# a proper error code and therefore the failure can't be detected and
	# the pid file is overwritten with the pid of a non-existing process
	if ! webag3status > /dev/null; then
		cd $webag3dir
		$JAVA $JAVA_OPTS -jar $WEBAG3_JAR $WEBAG3_OPTS 1>>$logfile 2>&1 &
		echo "$!" > $pidfile && success || failure
	else
		echo -n $"  already running"
		failure $"webag3 is already running"
	fi
	RETVAL=$?
	echo
}

stop()
{
	echo -n $"Stopping webag3:"
	if [ -n "`pidfileofproc webag3`" ]; then
		killproc webag3
	else
		failure $"Stopping webag3"
	fi
	RETVAL=$?
	echo
}

case "$1" in
	start)
		start
		;;
	stop)
		stop
		;;
	restart)
		stop
		start
		;;
	status)
		webag3status
		RETVAL=$?
		;;
	*)
		echo $"Usage: $0 {start|stop|restart|status}"
		RETVAL=1
esac
exit $RETVAL
