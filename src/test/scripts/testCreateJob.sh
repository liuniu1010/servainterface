curl -sS -X POST "http://172.17.0.1:18080/api/v1/aigamefactory/jobs" \
     -H "Content-Type: application/json; charset=utf-8" \
     -d '{"prompt":"Please create a basic calculator"}' \
     -o body.json \
     -D headers.txt \
     -w '%{http_code}\n' \
     > status.txt \
     2> curl.err

