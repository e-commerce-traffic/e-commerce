FROM redis:latest
COPY entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh
CMD ["sh", "/usr/local/bin/entrypoint.sh"]
