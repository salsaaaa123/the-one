#!/bin/bash
set -e  # Berhenti jika ada error

echo "Running compile.sh..."
./compile.sh

echo "Running one.sh..."
./one.sh -b 1 config/AlternateDropReport_settings.txt
./one.sh -b 1 config/Epidemic/
./one.sh -b 1 config/SprayAndWait/
./one.sh -b 1 config/Prophet/
