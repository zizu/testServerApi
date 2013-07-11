if [ -a RUNNING_PID ]
  then
    pid=`cat RUNNING_PID`
    echo "stopping play app"
    kill -SIGTERM $pid
fi
