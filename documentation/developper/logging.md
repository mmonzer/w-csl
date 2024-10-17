# Logs
## Log fields
There are some default fields that are logged by slf4j-logback automatically:
- `@timestamp` : timestamp in ISO8601.
- `@version` : version. Unused.
- `logger_name` : name of the class that generated the log.
- `thread_name` : name of the thread that generated the log.
- `level` : level of logs (INFO, WARN, ...).
- `level_value` : numeric value of level of logs (INFO=20000).

Other filed can be easily added with MDC variables. In our case:
 - `endpoint` : endpoint that generates the action that produces the log. In independent actions, a short description is given instead the endpoint.
 - `X-Correlation-ID` : correlation id between services. It allows to identify the requests/response for the same user action.  



## Log type
We have the usual log levels : TRACE, DEBUG, INFO, WARN and ERROR.
In addition, we have added a log_type depending on the nature of the log : either `network` either `applicative`.
- The `network` type will log only inbound and outbound requests/responses, with networking variables such as `ip_src` or `endpoint`.
- The `applicative` type will log everything related with the actions. It will have a message field more human-readable : "Scan started." or "Successfully synchronized HTTP connections."

## Rolling
Due to the quantity of networking logs, we have decided to save it aside, with a smaller rotation period.
This is done with the SiftAppender in the logback configuration file.