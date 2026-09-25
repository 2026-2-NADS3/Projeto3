# Sistema de Backup Automatizado em Bash

## Descrição

Este projeto consiste em um script Bash para automatizar o processo de backup dos dados do sistema Próxima Etapa no Linux. O script cria arquivos de backup compactados no formato `.tar.gz`, armazena os backups em um diretório definido pelo usuário, confere se o backup ficou completo e remove automaticamente os backups mais antigos, mantendo apenas os 3 mais recentes.

Projeto desenvolvido para a disciplina de Cloud Native (Entrega 1) do 3º semestre de Análise e Desenvolvimento de Sistemas. Os dados utilizados nos testes são fictícios, seguindo a LGPD.

## Funcionamento

O script realiza as seguintes etapas:

1. Valida os parâmetros informados pelo usuário.
2. Verifica se o diretório de origem existe.
3. Cria os diretórios de destino e de logs, caso necessário.
4. Cria o backup compactado no formato `.tar.gz`.
5. Verifica o código de retorno do `tar` para saber se o backup foi criado.
6. Confere se o backup possui a mesma quantidade de itens que o diretório de origem.
7. Registra o resultado da operação no arquivo de log.
8. Localiza os backups antigos e remove aqueles que ultrapassam a quantidade definida.
9. Registra a remoção dos backups antigos no arquivo de log.

## Requisitos

- Linux (testado no Ubuntu 24.04)
- Bash
- `tar`
- `find`
- `wc`
- `ls`
- `head`
- `rm`
- `mkdir`
- `date`

Todos os comandos já vêm instalados no Ubuntu.

## Estrutura do projeto

```
projeto-FLBJ/
├── README.md
├── scripts/
│   └── backup.sh
├── dados/
├── backups/
└── logs/
    └── backup.log
```

## Como executar

1. Entre na pasta do projeto:

```bash
cd ~/projeto-FLBJ
```

2. Dê permissão de execução ao script:

```bash
chmod u+x scripts/backup.sh
```

3. Execute informando o diretório de origem e o diretório de destino:

```bash
./scripts/backup.sh dados backups
```

> O script deve ser executado de dentro da pasta do projeto, pois utiliza caminhos relativos.

## Exemplos de uso

Backup realizado com sucesso:

```bash
./scripts/backup.sh dados backups
echo $?   # 0
```

Execução sem parâmetros:

```bash
./scripts/backup.sh
echo $?   # 2
```

Diretório de origem inexistente:

```bash
./scripts/backup.sh ausente backups
echo $?   # 1
```

## Códigos de saída

| Código | Significado |
|---|---|
| `0` | Backup realizado com sucesso |
| `1` | Diretório de origem não existe |
| `2` | Uso incorreto (parâmetros faltando) |
| `3` | Erro ao criar ou conferir o backup |

## Logs

Os logs são armazenados em `logs/backup.log`. Para visualizar as últimas linhas:

```bash
tail -n 10 logs/backup.log
```

Exemplo de saída:

```
[23/09/2026 23:34:49] backup criado: backups/backup_2026-09-23_23-34-49.tar.gz (4 itens)
[23/09/2026 23:34:49] apagado: backups/backup_2026-09-23_23-33-40.tar.gz
[23/09/2026 23:34:49] SUCESSO
[23/09/2026 23:34:49] ERRO: uso incorreto
[23/09/2026 23:34:49] ERRO: origem nao existe: ausente
```

## Resultado esperado

A cada execução, um novo arquivo é criado na pasta `backups` no formato:

```
backup_ANO-MES-DIA_HORA-MINUTO-SEGUNDO.tar.gz
```

Após várias execuções, apenas os 3 backups mais recentes permanecem na pasta.

## Prints

<p align="center">
<img src="imagens/" alt="Screenshot from 1.png" width="400">
<img src="imagens/" alt="Screenshot from 2.png" width="400">
<img src="imagens/" alt="Screenshot from 3.png" width="400">
<img src="imagens/" alt="Screenshot from 4.png" width="400">
</p>
