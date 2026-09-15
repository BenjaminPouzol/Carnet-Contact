import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ImageService } from './image';

describe('ImageService', () => {
  let service: ImageService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ImageService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  /**
   * Le piège classique de l'envoi de fichier : poser soi-même
   * `Content-Type: multipart/form-data`. Le navigateur n'y ajoute alors plus la
   * « frontière » entre les parties, et le serveur ne sait plus lire le corps.
   * Ce test vérifie qu'on le laisse faire.
   */
  it('envoie le fichier en FormData, sans Content-Type écrit à la main, et rend l\'URL', () => {
    const fichier = new File(['octets'], 'photo.png', { type: 'image/png' });
    let url: string | undefined;

    service.envoyer(fichier).subscribe(u => (url = u));

    const requete = backend.expectOne('/api/images');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toBeInstanceOf(FormData);
    expect(((requete.request.body as FormData).get('fichier') as File).name).toBe('photo.png');
    expect(requete.request.headers.has('Content-Type')).toBe(false);

    requete.flush({ url: 'http://localhost:8080/api/images/abc' });
    expect(url).toBe('http://localhost:8080/api/images/abc');
  });
});
