# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Every (class, method, descriptor) triple that the published 1.1.0 desktop artifacts exposed and
# that the 1.2.0 tree had dropped. Generated from a binary-compatibility-validator dump of the
# 1.1.0 jars; the consumer resolves each one against the candidate at run time.
#
# Columns: module<TAB>owner<TAB>method<TAB>descriptor<TAB>status
#   REQUIRED            - must resolve against the candidate
#   KNOWN_UNRESTORABLE  - compiler-synthesised, cannot be re-declared; see the compatibility notes
