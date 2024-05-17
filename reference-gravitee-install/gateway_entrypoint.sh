#!/bin/sh

# Copy the new plugin.zip file into the plugins directory
cp /mnt/plugins/*.zip ./plugins/

# Execute the original entrypoint command of the gateway container
exec ./bin/gravitee "$@"
