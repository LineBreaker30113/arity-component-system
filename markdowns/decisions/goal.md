# Goal

- ACS is meant to be a database that can be managed with commands. Specifically, the client is meant
  to start the "ACS engine", then must use the "ACS requester" as: `acsr boot` (starts the acs
  application), `acsr loadDB ~/main as main` (loads the database in the path main and allows access
  to it with the name of main), `acsr components main` (shows the components of the database).
  [The names changed afterwards: see Layers and names.]
- "acsr" is meant to work by editing, appending [to] a file of requests and prompting the
  application, which it will read and do.
- Then we will develop a GUI for that application that uses the terminal input (later we can isolate
  it from the terminal); also we will build an AI Agent that will use acsr. That is the goal.
- [Who is it for?] Since this is a good personal project, we can document it for gaining reputation.
- [What is a "paragraph of knowledge"?] It is more complicated [than a passage from one book, or a
  paragraph written from several books]. The first agent will build the personal knowledge graph from
  the books, then an auditing agent will audit it and improve, then a third agent will build a new
  acs of paragraphs from that first acs, which better describes and [is] copyright free. Also,
  paragraphs must cite the resources and reasoning to be accurate. We can talk about it later.
  *(The author wrote "gap" and confirmed "graph".)*
- [Who builds the structure, and who audits it?] Must allow agents at every step, since I want to
  automate it. Must allow humans in every step, since I want to study the procedure of developing
  proper knowledge graphs.
- [Better than what?] In every aspect.
- This database architecture is good specifically for RAG, since it is the most expressive by itself.
