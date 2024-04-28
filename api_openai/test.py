from urllib.parse import quote

def url_encode_filename(filename):
    return quote(filename)

# Example usage
filename = "Joe Bush файл.txt"
encoded_filename = url_encode_filename(filename)
print(encoded_filename)
