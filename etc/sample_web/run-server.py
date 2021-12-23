#! /usr/bin/env python3
import os, os.path, logging, subprocess
from threading import Thread
from flask import Flask, render_template, send_file

app = Flask(__name__, template_folder='.', static_folder='.')

@app.route('/')
def home() :
    try :
        return render_template('prime.html')
    except Exception as err :
        logging.error("render_template failed: %s", err)
        logging.warning("Current WD is %s", os.getcwd())
        exit(1)

@app.route('/<string:fname>')
def static_file(fname) :
    return send_file(fname)

def open_browser_thread(port) :
    import webbrowser, time

    time.sleep(0.2)

    url = f'http://localhost:{port}'
    logging.info("Opening %s", url)
    webbrowser.open(url)

def start() :
    from inetlab.cli.colorterm import add_coloring_to_emit_ansi

#     os.chdir(os.path.dirname(__file__))

    port = 9811

    logging.basicConfig(format="%(asctime)s.%(msecs)03d %(filename)s:%(lineno)d %(message)s",
                        level="DEBUG",
                        datefmt='%H:%M:%S')
    logging.StreamHandler.emit = add_coloring_to_emit_ansi(logging.StreamHandler.emit)

    Thread(target=open_browser_thread, args=(port,)).start ()

    logging.info("Starting in CLI debug mode")
    app.run(host='0.0.0.0', debug=True, port=port, use_reloader=False)


if __name__ == "__main__" :
    start ()
