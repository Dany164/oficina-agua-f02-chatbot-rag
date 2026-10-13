import Image from 'next/image'
function cn(...classes: Array<string | false | undefined>): string {
    return classes.filter(Boolean).join(' ')
}

type WaterMascotProps = {
  size?: 'xl' | 'lg' | 'sm'
  state?: 'idle' | 'thinking' | 'error'
  className?: string
}

export function WaterMascot({ size = 'lg', state = 'idle', className }: WaterMascotProps) {
  return (
    <span className={cn('water-mascot', `water-mascot--${size}`, `water-mascot--${state}`, className)}>
      <span className="water-mascot__halo" aria-hidden="true" />
      <span className="water-mascot__bubbles" aria-hidden="true"><i /><i /><i /><i /><i /><i /></span>
      <span className="water-mascot__float">
        <span className="water-mascot__tilt">
          <Image src="/images/water-mascot.webp" alt="Asistente virtual de la Oficina de Agua" fill priority={size === 'lg'} sizes={size === 'xl' ? '420px' : size === 'lg' ? '(max-width: 640px) 160px, 192px' : '44px'} className={cn('water-mascot__image', size === 'sm' && 'water-mascot__image--small')} />
        </span>
        <span className="water-mascot__shadow" aria-hidden="true" />
      </span>
      <span className="water-mascot__sparkles" aria-hidden="true"><i /><i /><i /></span>
    </span>
  )
}
