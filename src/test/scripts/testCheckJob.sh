curl -sS -X GET "http://172.17.0.1:18080/api/v1/aigamefactory/jobs/{jobId}" \
     -H "Content-Type: application/json; charset=utf-8" \
     -o body.json \
     -D headers.txt \
     -w '%{http_code}\n' \
     > status.txt \
     2> curl.err

