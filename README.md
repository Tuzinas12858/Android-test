Programos funkcionalumas:

AUDIO fragmentas:
  - MP3 failų ikėlimas iš internal storage
  - Ikeltų failų displayinimas list'e
  - MP3 failo paleidimas
  - Grojančio failo sustabdymas/sekančio failo paleidimas/praėjusio failo paleidimas
  - Grojančio failo praskipinimas pagal sliderį
  - MP3 failo pašalinimas iš sąrašo
  - Įkeltų failų kiekio rodymas (song x of y)
  - Programa prisimena prieš tai įkeltus failus, perkrovus programą lieką prieš tai įkeltas sąrašas MP3 failų

VIDEO fragmentas:
  - Nuotraukų ikėlimas iš internal storage
  - Nuotraukų displayinimas lentelėje
  - Įkeltų nuotraukų ištrinimas

COMBINED fragmentas:
  - Prašo pasirinkti nuotrauką ir audio failą
  - Pasirinkus nuotrauką ir audio failą šioje skilty galim pasirinkimus matyti
  - Galime paleisti audio failą
  - Pasirinkus nuotrauką ir audio failą galime spausti RENDER mygtuką, kuris inicijuoja video generavimo funkciją
  - Video generuojamas pagal šias gaires:
    - Padaro vaizdą juodai baltą
    - Pritaiko gaussian blur
    - Uždeda ant pagrindinės nuotraukos logotipą
    - Pritraukia nuotrauką 1:1 ratio 
  - Sėkmingai sukurto video duomenys perduodami google sheets per API, išsaugant Sukūrimo datą, Mp3 pavadinimą ir galutinio mp4 failo pavadinimą
VIDEOS fragmentas:
  - Galime matyti visus išrenderintus failus vidinėje atmintyje ieškant Movies/"APPSAS_**"
  - Paspaudus ant failo jį galima peržiūrėti
