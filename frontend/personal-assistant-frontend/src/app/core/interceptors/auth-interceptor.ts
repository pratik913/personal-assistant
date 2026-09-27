import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';

import {
  catchError,
  throwError
} from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (
  req,
  next
) => {

  const token =
    localStorage.getItem('access_token') ??
    sessionStorage.getItem('access_token');

  console.log(
    '[AUTH INTERCEPTOR]',
    req.method,
    req.url,
    'TOKEN:',
    token ? 'PRESENT' : 'MISSING'
  );

  const authenticatedRequest = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  console.log(
    '[AUTH INTERCEPTOR]',
    'Authorization:',
    authenticatedRequest.headers.has('Authorization')
      ? 'ATTACHED'
      : 'NOT ATTACHED'
  );

  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {

      if (
        error.status === 401 &&
        token
      ) {
        localStorage.removeItem('access_token');
        sessionStorage.removeItem('access_token');
      }

      return throwError(() => error);
    })
  );
};