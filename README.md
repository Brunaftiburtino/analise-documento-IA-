# Validação de documentos com Java

API didática em Spring Boot para demonstrar um pipeline de validação. O serviço recebe texto ou imagem, pode pré-processar a imagem com OpenCV e usar Tesseract para OCR, extrai campos com regras locais e aplica as regras de negócio. A integração com OpenAI será adicionada em etapa seguinte.

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

## Pré-processamento com OpenCV

Opcionalmente, configure `app.opencv.command` com um comando externo que receba o caminho do arquivo de entrada e o caminho de saída, nessa ordem. Por exemplo, no PowerShell:

```powershell
$env:APP_OPENCV_COMMAND="python scripts/preprocessar_documento.py"
```

O script deve ler o primeiro argumento, aplicar o pré-processamento usando OpenCV e salvar a imagem tratada no segundo argumento. Exemplo de configuração completa:

```python
import cv2
import sys

imagem = cv2.imread(sys.argv[1])
cinza = cv2.cvtColor(imagem, cv2.COLOR_BGR2GRAY)
tratada = cv2.threshold(cinza, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]
if not cv2.imwrite(sys.argv[2], tratada):
    raise RuntimeError("Não foi possível salvar a imagem pré-processada")
```

O comando é iniciado sem shell. Se o caminho do executável ou de algum argumento contiver espaços, coloque esse trecho entre aspas no valor da propriedade. O processo tem limite de 60 segundos. Se o OpenCV não estiver configurado, a imagem original segue para o OCR.

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

O OCR aceita imagens PNG, JPEG, TIFF e BMP. Quando configurado, o pré-processamento OpenCV ocorre antes do Tesseract. Ambos são executados localmente, sem passar argumentos por um shell.

Se o Tesseract não estiver configurado e o request contiver `conteudoDocumento`, esse texto continua sendo usado como fallback. Sem texto e sem OCR configurado, a API retorna `400 Bad Request`. A extração estruturada continua sendo local e baseada em padrões de texto; a integração com OpenAI ainda não foi implementada.
