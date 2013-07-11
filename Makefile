run:
	play clean compile stage
	nohup target/start > /dev/null 2>&1 &

