run:
	play clean compile stage
	bash kill_previous.sh
	nohup target/start > /dev/null 2>&1 &

stop:
	bash kill_previous.sh
