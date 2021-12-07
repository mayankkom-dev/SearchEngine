from gensim.models.doc2vec import Doc2Vec, TaggedDocument
from gensim.models.word2vec import Word2Vec
from nltk.tokenize import word_tokenize
from flask import Flask, request, jsonify, render_template, send_from_directory
import pandas as pd
import os
from flask_cors import CORS
import itertools
import gensim.downloader
import pickle
import joblib
import random, re
from nltk.corpus import stopwords
stp = stopwords.words("english")
from gensim.models.callbacks import CallbackAny2Vec

# init callback class
class callback(CallbackAny2Vec):
    """
    Callback to print loss after each epoch
    """
    def __init__(self):
        self.epoch = 0
        self.model_dir = "gensim-models"

    def on_epoch_end(self, model):
        loss = model.get_latest_training_loss()
        
        if self.epoch == 0:
            print('Loss after epoch {}: {}'.format(self.epoch, loss))
        elif self.epoch % 10 == 0:
            l = loss- self.loss_previous_step
            print('Loss after epoch {}: {}'.format(self.epoch, l))
            model.save(f'{self.model_dir}/{self.epoch}-{l}.model')
            print(f"Saved model successfully {self.epoch}-{l}")
        
        self.epoch += 1
        self.loss_previous_step = loss

word_model = Word2Vec.load(os.path.join("Word2vec", 'w2vec_smart.model'))
vectorizerF = joblib.load("vectorizerF.pkl")
km = joblib.load("km.pkl")

with open("k-means-doc2vec_DBOW.sav", "rb") as fp:
    k = pickle.load(fp)
# with open("gensim.pkl", "rb") as fp:
#    glove_vectors = pickle.load(fp)
#glove_vectors = api.load("glove-wiki-gigaword-50")

with open("magic_dmp.pkl", "rb") as fp:
    magic_dmp = pickle.load(fp)

app = Flask(__name__, static_folder="static")
CORS(app)

model = Doc2Vec.load(r'DocToVector_200_DBOW.model')
#model = Doc2Vec.load('DocToVector.model')


def prep_smart(inp):
    inp = inp.lower()
    inp = re.sub("[^a-zA-Z\n]+"," ", inp)
    inp = re.sub('\n+',"\n", inp)
    # finding these at beginning of sentence
    re_abs = re.search('\n\s*abstract', inp)
    re_intro = re.search('\n\s*introduction', inp)
    re_ref = re.search('\n\s*references?', inp)
    st_end = [[-1,-1], [-1,-1], [-1,-1]]
    
    for idx, rt in enumerate([re_abs, re_intro, re_ref]):
        while rt:
            st_end[idx] = [rt.start(), rt.end()]
            break
    # st = st_end[0][1] if st_end[0][1]>0 else st_end[1][1]
    # en = st_end[-1][0] if st_end[-1][0]>0 else None
    #modifying start and end to keep till introduction only
    
    if st_end[1][1]>0:
        print("Keeping till introduction")
        st = 0
        en = st_end[1][0]
    elif st_end[0][1]>0:
        print("Keeping till abstract")
        st = 0
        en = st_end[0][0]
    elif st_end[-1][0]>0:
        print("Fetching till references")
        st = 0
        en = st_end[-1][0]
    else:
        print("using full paper")
        st = 0
        en = len(inp)
    doc = inp[st:en]
    # st = st if st>0 else 0
    # if en:
    #     doc= inp[st:en]
    # else:
    #     doc = inp[st:]
    doc = re.sub('\n+'," ", doc)
    doc = " ".join(w for w in doc.split() if w.strip() and w not in stp and len(w)>4)
    return doc
    
def prep_simp(text):
    return re.sub("[^a-zA-Z\n]+", " ", text)

with open("trans_cls.pkl", "rb") as fp:
    vectorizerC = pickle.load(fp)

with open("nb_cls.pkl", "rb") as fp:
    nb_cls = pickle.load(fp)

def try_magic(fp):
    with open(fp, encoding="utf") as f:
        cont = f.read()
    cont = prep_inp(cont)
    vec = vectorizerF.transform([cont])
    cl = km.predict(vec)[0]
    return random.sample(magic_dmp[cl][:30], k=10)

def prep_inp(inp):
    inp = inp.lower()
    inp = re.sub("[^a-zA-Z\n]+", " ", inp)
    inp = re.sub('\n+', "\n", inp)
    doc = inp
    doc = re.sub('\n', ' ', doc)
    doc = re.sub('  ', ' ', doc)
    return doc

app = Flask(__name__, static_folder="static")
CORS(app)

# glove_vector = gensim.downloader.load("fasttext-wiki-news-subwords-300")
# model = Doc2Vec.load(r'D:\Mayank\IR\Test\DocToVector_200_DBOW.model')
# model = Doc2Vec.load(r'DocToVector.model')
# with open("gensim.pkl", "rb") as fp:
#     glove_vector = pickle.load(fp)

def query_expansion(inp_query):
    words = [w for w in inp_query.split() if w.strip()]
    if len(words)<4:
        w = "_".join(words)
        print(w)
        try:
            t = word_model.wv.most_similar(w, topn=4)
            print("here", t)
            return [" ".join(w.split("_")) for w,s in t]
        except:
            expp = []
            for w in words:
                try:
                    t = word_model.wv.most_similar(w, topn=10)
                except:
                    t = []
                if t:
                    expp.append([wr for wr,s in random.sample(t, k=2)])
            if len(expp)==len(words):
                return [" ".join(i) for i in list(itertools.product(*expp))]
    
   
        
    return []
        
def get_similar_doc(in_fpath):
#     base_path = r"D:\Mayank\IR\phase2"
    with open( in_fpath, encoding='utf8') as input_file:
        content = input_file.read()
    
    prep_cls = prep_smart(content)
    cls = nb_cls.predict(vectorizerC.transform([prep_cls]))[0]
    cls = "Software" if cls=="icse" else "Database"
    
    clstr = word_tokenize(prep_cls)
    veclt = model.infer_vector(clstr)
    clst = str(k.predict([veclt])[0])
    ## do classification and clusterring here
    
    test = word_tokenize(content.lower())
    vec = model.infer_vector(test)
    sim_docs = model.docvecs.most_similar(positive=[vec], topn=10)[1:]
    rel_terms = try_magic(in_fpath)
    t_ = [{"p_": "/".join(doc.split("/")[-3:]), "r_t": rel} for (doc,sc),rel in zip(sim_docs,rel_terms)]
    t_.append(clst)
    t_.append(cls)
    return t_

@app.route('/expQ', methods=['POST'])
def expQ():
    inp_query = request.form.get('queryTerm')
    print(inp_query)
    exp_query = []
    if len(inp_query.split())>1:
        exp_query = query_expansion(inp_query)
    return jsonify(exp_query)

@app.route('/simDoc', methods=['POST'])
def sim_doc():
    in_fpath = request.form.get('in_fpath')[1:]
    print(in_fpath)
#     in_fpath = "\\".join(in_fpath.split("\\")[-3:])
    sim_docs = get_similar_doc(in_fpath)
    return jsonify(sim_docs)

@app.route('/', methods=['GET','POST'])
def index_pg():
    return render_template(r"VueSample.htm")

if __name__ == "__main__":
    app.run(host="0.0.0.0")
