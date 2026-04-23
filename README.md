# PDI — Processamento Digital de Imagens

Projeto desenvolvido para a disciplina de **Processamento Digital de Imagens**, com o objetivo de implementar uma aplicação gráfica capaz de aplicar transformações e filtros em imagens.

## 📌 Descrição

O sistema consiste em uma aplicação desktop em Java (Swing), que permite:

- Carregar uma imagem
- Visualizar a imagem original
- Aplicar transformações e filtros
- Visualizar o resultado em tempo real
- Salvar a imagem transformada

A interface foi projetada com menus organizados e duas áreas principais:
- 📷 Imagem Original
- 🎨 Imagem Transformada

---

## 🧑‍💻 Autores

- Vitor Matheus Scheffler

---

## 📁 Estrutura do Projeto

```
📦 PDI
 ┣ 📁 src/
 ┃ ┣ 📜 PDI.java        # Interface gráfica e controle
 ┃ ┗ 📜 Opcoes.java     # Implementação das operações de imagem
 ┗ 📁 img/              # (Opcional) Pasta padrão para abrir imagens
```

---

## 🖥️ Interface do Sistema

A interface possui:

- Menu superior com todas as funcionalidades
- Duas janelas principais lado a lado:
  - Exibição da imagem original
  - Exibição da imagem transformada
- Possibilidade de expansão para múltiplas janelas (pipeline de transformações)

---

## 📂 Funcionalidades

### 📁 Menu Arquivo
- Abrir imagem
- Salvar imagem
- Sobre
- Sair

---

### 🔷 Transformações Geométricas
- Translação
- Rotação
- Espelhamento:
  - Horizontal
  - Vertical
- Escala:
  - Aumentar
  - Diminuir

---

### 🎨 Filtros
- Grayscale (3 métodos)
- Ajuste de Brilho
- Ajuste de Contraste
- Threshold (binarização)
- Passa Baixa (Gaussiano)
- Passa Alta:
  - Sobel
  - Canny

---

### 🧠 Morfologia Matemática
> 🚧 Estrutura preparada para implementação futura

- Dilatação
- Erosão
- Abertura
- Fechamento
- Afinamento

---

### 🔍 Extração de Características
> 🚧 Em definição (Desafio)

- Espaço reservado para técnicas avançadas

---

## ⚙️ Tecnologias Utilizadas

- Java
- Java Swing (Interface gráfica)
- BufferedImage (Manipulação de imagens)
