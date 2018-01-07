FROM java:8

ENV REDIS_HOST 172.17.0.1 
ENV STORAGE_LOCATION /root/cfp-devoxxma/storage
ENV STORAGE_API http:xhub.ddns.net:9000/storage

WORKDIR /root 

EXPOSE 9000

COPY  target/universal/build/cfp-devoxxma-* /root/cfp-devoxxma

RUN chmod +x /root/cfp-devoxxma/bin/cfp-devoxxma

CMD ["/root/cfp-devoxxma/bin/cfp-devoxxma"]

VOLUME /apps/storage:$STORAGE_LOCATION

