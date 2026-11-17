# Documentazione

## Autori

- Mattia Senni (mattia.senni@studio.unibo.it)
- Fabio Fattori (fabio.fattori3@studio.unibo.it)
- Francesco Tonelli (francesco.tonelli9@studio.unibo.it)

## Esercizio 1

### Analisi

Durante l'analisi della traccia proposta sono stati considerati i seguenti aspetti critici:
- il sistema di allarme è composto da elementi che possono essere eseguiti su JVM e nodi diversi. Non esiste una memoria condivisa affidabile dalla quale leggere lo stato globale, quindi sensori, zone, unità di controllo e interfaccia devono coordinarsi tramite messaggi;
- eventi come rilevazioni, inserimento del PIN e scadenze di timeout possono avvenire quasi contemporaneamente;
- gli stati del sistema determinano quali messaggi possono produrre un effetto. Le transizioni, quindi, devono essere elaborate in ordine e in modo atomico, altrimenti una rilevazione o un timeout potrebbero essere gestiti rispetto a uno stato non più valido; 
- dopo un riavvio non è possibile ricostruire con certezza lo stato precedente. Per questo il sistema parte da RECOVERY, ignora le rilevazioni dei sensori e accetta come unica transizione sicura l'inserimento del PIN corretto.

### Sviluppo

Il sistema è stato realizzato con Apache Pekko seguendo il modello ad attori. 
- ogni attore possiede il proprio stato e lo modifica solo durante la gestione di un messaggio;
- la mailbox serializza l'elaborazione e permette di evitare l'accesso concorrente diretto alle strutture dati;
- la comunicazione tra nodi usa messaggi serializzabili e `ActorSelection`, mantenendo separata l'identità logica di un attore dalla sua posizione fisica;
- a distribuzione è divisa nei ruoli control, zones e sensors; 
- l'interfaccia JavaFX viene eseguita localmente con un UIActor che inoltra i comandi e riceve gli aggiornamenti; 
- la `ControlUnitActor` contiene la macchina a stati e programma i ritardi di ingresso e uscita tramite lo scheduler di Pekko;
- la scadenza viene trasformata in un normale messaggio diretto all'attore stesso, quindi viene ordinata insieme agli altri eventi. 
- `ZoneManagerActor` possiede l'associazione tra zone e sensori e crea un attore per ogni zona. Una zona inoltra una rilevazione solo quando è attiva.

Gli aggiornamenti grafici sono eseguiti con `Platform.runLater`, perché i messaggi degli attori non vengono gestiti dal JavaFX Application Thread. È stata impostata una configurazione Docker, la quale avvia i tre ruoli in JVM separate, rendendo effettiva la distribuzione senza introdurre oggetti condivisi tra i nodi.

## Esercizio 2

### Analisi

Durante l'analisi della traccia proposta sono stati considerati i seguenti aspetti critici:
- il server deve gestire più partite e più client contemporaneamente, mantenendo un'unica versione autorevole dello stato di ogni partita; 
- la creazione di due partite con lo stesso nome, l'ingresso simultaneo di più giocatori e due mosse concorrenti devono quindi essere risolti atomicamente sul server. 
- una mossa comprende più controlli che non possono essere separati. Bisogna verificare identità del giocatore, turno, posizione e stato della partita prima di aggiornare la board;
- il cambio del turno, il controllo della vittoria e l'avvio del timeout successivo fanno parte anch'essi di una transizione; 
- il client non deve bloccare il thread della GUI;
- il client deve ricevere gli aggiornamenti dell'avversario e quelli generati dai timer senza effettuare polling.

### Sviluppo

Il software prodotto presenta i seguenti aspetti:
- il contratto distribuito è raccolto nel package `common`, che contiene soltanto le interfacce remote e i dati condivisi. Le chiamate tra client e server dipendono unicamente da questo contratto.
- il registry pubblica un unico `RemoteLobby`. La creazione di una partita produce e restituisce un nuovo `RemoteGame`, mentre il join recupera lo stesso oggetto remoto associato al nome scelto. 
- la mappa delle partite e le operazioni che modificano una singola partita sono sincronizzate, così ogni controllo e il relativo aggiornamento costituiscono una sola operazione atomica;
- lo stato viene trasferito con `Match`, uno snapshot immutabile che contiene board, giocatori, turno e stato della partita;
- ogni client esporta un `RemoteGameListener` e lo registra nel `RemoteGame`. Quando avviene un join, una mossa, un timeout o la chiusura della partita, il server invia agli interessati un nuovo snapshot completo. Questa soluzione riprende il pattern **observer con remote listener** e non richiede busy waiting;
- un executor a singolo thread per ogni partita mantiene le notifiche e impedisce che una callback remota rallenti la sezione che aggiorna lo stato: se una di queste callback fallisce, il listener viene rimosso;
- le chiamate RMI in uscita dal client sono eseguite da un worker dedicato; 
- gli aggiornamenti ricevuti dalle callback vengono invece trasferiti al JavaFX Application Thread con `Platform.runLater`;
- sul server ogni turno ha un timeout di 20 secondi. Questo aspetto è stato aggiunto per evitare l'attesa infinita di un giocatore inattivo. Allo scadere del timer, viene concessa la vittoria all'altro utente.
- alla fine della partita il risultato viene notificato, poi la partita viene chiusa dopo un breve intervallo, rimossa dalla lobby e notificata nuovamente come chiusa. I client possono così tornare alla selezione senza interrogare periodicamente il server.

## Esercizio 3

### Analisi

L'obiettivo è offrire una sezione critica a processi distribuiti senza esporre all'applicazione i dettagli degli altri partecipanti. Non essendoci memoria condivisa né un clock globale, un normale lock locale non può garantire la mutua esclusione tra processi diversi: serve quindi un protocollo che ordini le richieste e conceda l'accesso a un solo processo alla volta, tramite lo scambio di messaggi.

Le proprietà rilevanti sono: 
- safety, per impedire due accessi simultanei;
- liveness, per non lasciare una richiesta in attesa senza motivo; 
- fairness, per ordinare richieste concorrenti.

L'implementazione deve coordinare il thread che chiama `acquire` o `release` con il thread usato da RabbitMQ per consegnare i messaggi.

### Sviluppo

Il middleware implementa l'algoritmo decentralizzato di Ricart-Agrawala. 
- ogni processo mantiene gli stati RELEASED, WANTED e HELD e un clock logico di Lamport. Quando richiede il lock, incrementa il clock, associa il timestamp alla richiesta e invia un messaggio REQUEST a tutti i peer conosciuti;
- il chiamante attende su un monitor fino a quando non riceve un REPLY da ciascun peer, senza eseguire busy waiting; 
- alla ricezione di una richiesta, un processo risponde subito se non possiede la risorsa e non ha una richiesta con precedenza;
  - se si trova in HELD, oppure la propria richiesta in WANTED ha un timestamp minore, la risposta viene rimandata. 
  - in caso di timestamp uguali viene confrontato l'identificativo del processo, ottenendo un ordine totale deterministico. 
- con `release` tutte le risposte differite vengono inviate e il lock torna disponibile. 
- l'accesso allo stato, all'insieme delle risposte mancanti e alla coda differita è protetto dallo stesso monitor.

RabbitMQ è incapsulato dietro `TransportService`:
- un exchange fanout viene usato per i messaggi HELLO di scoperta, mentre un exchange direct consegna REQUEST e REPLY al processo destinatario;
- ogni istanza possiede una coda temporanea e serializza i messaggi in JSON: questa separazione mantiene l'algoritmo indipendente dal trasporto;
- il clock e le operazioni di invio sono sincronizzati, perché possono essere usati sia dal flusso principale sia dalle callback di RabbitMQ. 

Il programma dimostrativo attende una prima fase di scoperta, acquisisce il lock, esegue la sezione critica e resta disponibile per rispondere alle richieste degli altri processi.
