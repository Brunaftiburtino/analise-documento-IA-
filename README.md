# Validação de documentos com Java

API didática em Spring Boot para demonstrar um pipeline de validação. Nesta primeira etapa, o serviço recebe texto, extrai campos com regras locais e aplica as regras de negócio. OpenCV, Tesseract e OpenAI serão adicionados em etapas seguintes.

## Requisitos e execução

- Java 25+
- Maven 3.9+

```powershell
mvn spring-boot:run
```

## Endpoints

### `GET /api/documentos/exemplo`

Retorna um payload de exemplo com a data de emissão atual.

### `POST /api/documentos/validar`

Exemplo de request:

```json
{
  "nomeArquivo": "comprovante-residencia.pdf",
  "mimeType": "application/pdf",
  "conteudoDocumento": "COMPROVANTE DE RESIDENCIA\nNome: Joao da Silva\nCPF: 123.456.789-00\nEndereco: Rua das Flores, 150\nCidade: Guanambi - BA\nData de emissao: 04/11/2026",
  "imagemBase64": null
}
```

Nesta etapa, envie `conteudoDocumento`. O campo `imagemBase64` já faz parte do contrato para a futura etapa de OCR, mas imagem sem texto ainda não é processada.

O retorno inclui os campos extraídos, as etapas do pipeline e o resultado das regras:

- nome, CPF, endereço, cidade e data de emissão;
- aprovação quando todos os campos são encontrados e a emissão ocorreu nos últimos 90 dias;
- pendências para campos ausentes ou documentos antigos.

O processamento local é um fallback didático baseado em padrões de texto; ainda não chama OpenAI, Tesseract nem OpenCV.
