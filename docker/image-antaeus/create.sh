#!/bin/bash
docker run -h antaeus -p 28889:28888 -e ZOOKEEPER_CONNECT="192.168.1.6:21181" --name antaeus --link  docker_kafka1_1:kafka1 --net docker_default -it 192.168.1.5:5000/image-antaeus
