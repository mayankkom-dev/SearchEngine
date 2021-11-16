from gensim.models.doc2vec import Doc2Vec, TaggedDocument
from nltk.tokenize import word_tokenize
from flask import Flask, request, jsonify
import pandas as pd
import os
from flask_cors import CORS
from flask import send_from_directory, render_template
import pickle
import joblib
import random
import re
import itertools
from gensim.models.word2vec import Word2Vec


word_model = Word2Vec.load(os.path.join("Word2vec", 'WordToVector_32_SG.model'))
vectorizerF = joblib.load("vectorizerF.pkl")
km = joblib.load("km.pkl")

# with open("gensim.pkl", "rb") as fp:
#    glove_vectors = pickle.load(fp)
#glove_vectors = api.load("glove-wiki-gigaword-50")

with open("magic_dmp.pkl", "rb") as fp:
    magic_dmp = pickle.load(fp)

app = Flask(__name__, static_folder="static")
CORS(app)

model = Doc2Vec.load(r'DocToVector_200_DBOW.model')
#model = Doc2Vec.load('DocToVector.model')


def query_expansion(inp_query):
    words = [w for w in inp_query.split() if w.strip()]
    expp = []
    for w in words:
        try:
            t = word_model.wv.most_similar(w, topn=5)
        except:
            t = []
        if t:
            expp.append([wr for wr, s in random.sample(t, k=2)])
            print(expp)
    if len(expp) == len(words):
        return [" ".join(i) for i in list(itertools.product(*expp))]
    return []


## as pre-processing
# taking of anything in between abstract/introduction(nothing else on line) till reference
def prep_inp(inp):
    inp = inp.lower()
    inp = re.sub("[^a-zA-Z\n]+", " ", inp)
    inp = re.sub('\n+', "\n", inp)
    # finding these at beginning of sentence
    re_abs = re.search('\n\s*abstract', inp)
    re_intro = re.search('\n\s*introduction', inp)
    re_ref = re.search('\n\s*references?', inp)
    st_end = [[-1, -1], [-1, -1], [-1, -1]]

    for idx, rt in enumerate([re_abs, re_intro, re_ref]):
        while rt:
            st_end[idx] = [rt.start(), rt.end()]
            break
    st = st_end[0][1] if st_end[0][1] > 0 else st_end[1][1]
    en = st_end[-1][0] if st_end[-1][0] > 0 else None

    st = st if st > 0 else 0
    if en:
        doc = inp[st:en]
    else:
        doc = inp[st:]
    doc = re.sub('\n+', " ", doc)
    return doc

def prepDoc_inp(inp):
    inp = inp.lower()
    inp = re.sub("[^a-zA-Z\n]+", " ", inp)
    inp = re.sub('\n+', "\n", inp)
    doc = inp
    doc = re.sub('\n', ' ', doc)
    doc = re.sub('  ', ' ', doc)
    return doc

def get_similar_doc(in_fpath):
    with open(in_fpath, encoding='utf8') as input_file:
        content = input_file.read()
    content = prepDoc_inp(content)
    test = word_tokenize(content.lower())

    vec = model.infer_vector(test)
    sim_docs = model.docvecs.most_similar(positive=[vec], topn=5)
    rel_terms = try_magic(in_fpath)
    return [{"p_": "/".join(doc.split("/")[-3:]), "r_t": rel} for (doc, sc), rel in zip(sim_docs, rel_terms)]


def try_magic(fp):
    with open(fp, encoding="utf") as f:
        cont = f.read()
    cont = prep_inp(cont)
    vec = vectorizerF.transform([cont])
    cl = km.predict(vec)[0]
    return random.sample(magic_dmp[cl][:30], k=10)


@app.route('/simDoc', methods=['POST'])
def sim_doc():
    in_fpath = request.form.get('in_fpath')[1:]
    print(in_fpath)

    sim_docs = get_similar_doc(in_fpath)
    return jsonify(sim_docs)


@app.route('/expQ', methods=['POST'])
def expQ():
    inp_query = request.form.get('queryTerm')
    print(inp_query)
    exp_query = []
    if len(inp_query.split()) > 1:
        exp_query = query_expansion(inp_query)
    return jsonify(exp_query)


@app.route('/', methods=['GET', 'POST'])
def index_pg():
    return render_template(r"VueSample.htm")


if __name__ == "__main__":
    app.run(host="0.0.0.0")
