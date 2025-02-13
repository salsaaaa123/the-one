#!/bin/bash
set -e  # Berhenti jika ada error

echo "Running compile.sh..."
./compile.sh

echo "Running one.sh..."
./one.sh -b 1 properties/AlternateDropReport_settings.txt
