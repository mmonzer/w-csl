# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information
import os
import sys

sys.path.insert(0, os.path.abspath("themes"))


project = 'W-CSL'
root_doc='index'

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = ['myst_parser']

exclude_patterns = ['build', 'Thumbs.db', '.DS_Store','.venv','themes','APIDOC.rst', 'JAVADOC.rst', 'index.default.rst']
unused_docs=['APIDOC', 'JAVADOC']

intersphinx_disabled_reftypes = ["*"]

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = 'rtd_modified'
html_theme_path = ["themes"]
