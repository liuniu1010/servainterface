docker run -d \
-p <serverip>:28080:8080 \
--add-host=mydb:<dbip> \
--name servainterface2 \
--restart=unless-stopped \
servainterface2:0.1
