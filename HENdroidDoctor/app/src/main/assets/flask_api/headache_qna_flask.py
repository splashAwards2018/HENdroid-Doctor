# pip install flask if not installed
# don't name your file as flask.py !
from flask import Flask, redirect, url_for, request, render_template
from bs4 import BeautifulSoup as bs
import requests

# create a Flask object
app = Flask(__name__)

# function to access website to get the answer given the quesiton
def response(symptom):    
    symptom = "-".join(symptom.split())
    url = 'https://www.webmd.com/migraines-headaches/qa/' + symptom
    response = requests.get(url)
    soup = bs(response.text, 'html.parser')
    cond = soup.find('div', {'id' : 'art-ans'}).text.strip().replace('\n', " ")
    print (cond)
    return cond

# load the symptom webpage
@app.route('/')
def load_webpage():
    return render_template('symptom.html')  

# get the condition
@app.route('/symptom', methods = ['POST'])
def symptom():    
    if request.method == 'POST':
        symptom = request.form['symptom']
        reply = response(symptom)
        return redirect(url_for('cond', condition = reply))

# display the condition 
@app.route('/cond/<condition>')
def cond(condition):
    return ('Answer : %s' % condition)

# run the app
if __name__ == '__main__':
    app.run(debug = True)


