# Validação de documentos com Java

API didática em Spring Boot para demonstrar um pipeline de validação. O serviço recebe texto ou imagem, usa Tesseract opcionalmente para OCR, extrai campos com regras locais e aplica as regras de negócio. OpenCV e OpenAI serão adicionados em etapas seguintes.

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

Envie `conteudoDocumento` para usar o fallback local ou envie uma imagem em `imagemBase64` após configurar o Tesseract. `mimeType` deve corresponder ao formato da imagem.

O retorno inclui os campos extraídos, as etapas do pipeline e o resultado das regras:

- nome, CPF, endereço, cidade e data de emissão;
- aprovação quando todos os campos são encontrados e a emissão ocorreu nos últimos 90 dias;
- pendências para campos ausentes ou documentos antigos.

## OCR com Tesseract

Para processar uma imagem enviada em `imagemBase64`, configure o executável do Tesseract. No PowerShell:

```powershell
$env:APP_OCR_COMMAND="tesseract"
mvn spring-boot:run
```

Opcionalmente, configure `app.ocr.language` (padrão `por`) e `app.ocr.tessdata-path` caso os dados de idioma não estejam no caminho padrão. Por exemplo, essas propriedades também podem ser definidas em `src/main/resources/application.properties`:

```properties
app.ocr.command=tesseract
app.ocr.language=por
app.ocr.tessdata-path=C:/tesseract/tessdata
```

O OCR aceita imagens PNG, JPEG, TIFF e BMP. A aplicação executa o Tesseract localmente, sem passar argumentos por um shell.

Se o Tesseract não estiver configurado e o request contiver `conteudoDocumento`, esse texto continua sendo usado como fallback. Sem texto e sem OCR configurado, a API retorna `400 Bad Request`. Esta etapa ainda não integra OpenCV nem OpenAI; a extração estruturada continua sendo local e baseada em padrões de texto.
